package com.cityroulette.ingestion.tourapi;

import com.cityroulette.integration.tourapi.TourismApiException;
import com.cityroulette.ingestion.history.ImportRun;
import com.cityroulette.domain.place.entity.Place;
import com.cityroulette.ingestion.tourapi.dto.TourismCollectionResult;
import com.cityroulette.integration.tourapi.dto.TourismPage;
import com.cityroulette.integration.tourapi.dto.TourismPlaceDto;
import com.cityroulette.ingestion.history.ImportRunRepository;
import com.cityroulette.domain.place.repository.PlaceMergeCandidateRepository;
import com.cityroulette.domain.place.repository.PlaceRepository;
import com.cityroulette.domain.place.repository.PlaceSourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class TourismCollectionServiceTest {
    @Autowired TourismCollectionService collectionService;
    @Autowired PlaceRepository placeRepository;
    @Autowired PlaceSourceRepository sourceRepository;
    @Autowired PlaceMergeCandidateRepository candidateRepository;
    @Autowired ImportRunRepository runRepository;
    @MockitoBean TourismService tourismService;

    @BeforeEach
    void clean() {
        candidateRepository.deleteAll();
        sourceRepository.deleteAll();
        placeRepository.deleteAll();
        runRepository.deleteAll();
    }

    @AfterEach
    void cleanAfter() {
        clean();
    }

    @Test
    void paginatesUpToLimitAndIsIdempotentOnRerun() {
        when(tourismService.getBusanPlacesPage(1, 2))
                .thenReturn(new TourismPage(List.of(dto("1"), dto("2")), 1, 2, 3));
        when(tourismService.getBusanPlacesPage(2, 2))
                .thenReturn(new TourismPage(List.of(dto("3")), 2, 2, 3));

        TourismCollectionResult first = collectionService.collect(false, 2, 3);
        TourismCollectionResult second = collectionService.collect(false, 2, 3);

        assertThat(first.inserted()).isEqualTo(3);
        assertThat(first.pagesRequested()).isEqualTo(2);
        assertThat(second.inserted()).isZero();
        assertThat(second.updated()).isEqualTo(3);
        assertThat(second.duplicates()).isEqualTo(3);
        assertThat(placeRepository.count()).isEqualTo(3);
        assertThat(sourceRepository.count()).isEqualTo(3);
    }

    @Test
    void dryRunReportsInsertWithoutChangingPlaceTables() {
        when(tourismService.getBusanPlacesPage(1, 1))
                .thenReturn(new TourismPage(List.of(dto("10")), 1, 1, 1));

        TourismCollectionResult result = collectionService.collect(true, 1, 1);

        assertThat(result.dryRun()).isTrue();
        assertThat(result.inserted()).isEqualTo(1);
        assertThat(placeRepository.count()).isZero();
        assertThat(sourceRepository.count()).isZero();
        assertThat(result.runId()).isEqualTo(-1L);
        assertThat(runRepository.count()).isZero();
    }

    @Test
    void recordsFailedRunWhenApiRequestFails() {
        doThrow(new TourismApiException("표본 호출 실패"))
                .when(tourismService).verifyBusanLegalDongCode();

        assertThatThrownBy(() -> collectionService.collect(false, 10, 10))
                .isInstanceOf(TourismApiException.class);

        ImportRun failed = runRepository.findAll().get(0);
        assertThat(failed.getStatus()).isEqualTo("FAILED");
        assertThat(failed.getFailedCount()).isEqualTo(1);
        assertThat(failed.getErrorMessage()).isEqualTo("표본 호출 실패");
        assertThat(placeRepository.count()).isZero();
    }

    @Test
    void recordsSimilarManualPlaceAsCandidateWithoutAutoMerging() {
        placeRepository.save(new Place("부산 장소 20", "관광지",
                "부산광역시 해운대구 해운대로 1", 35.16, 129.16, null));
        when(tourismService.getBusanPlacesPage(1, 1))
                .thenReturn(new TourismPage(List.of(dto("20")), 1, 1, 1));

        TourismCollectionResult result = collectionService.collect(false, 1, 1);

        assertThat(result.inserted()).isEqualTo(1);
        assertThat(placeRepository.count()).isEqualTo(2);
        assertThat(candidateRepository.count()).isEqualTo(1);
    }

    private TourismPlaceDto dto(String id) {
        return new TourismPlaceDto(id, "12", "부산 장소 " + id,
                "부산광역시 해운대구", "해운대로 1", "129.16", "35.16",
                "https://example.com/" + id + ".jpg", "", "Type1", "26", "350",
                "VE", "VE01", "VE010100", "", "", "20260101000000", "20261001000000");
    }
}
