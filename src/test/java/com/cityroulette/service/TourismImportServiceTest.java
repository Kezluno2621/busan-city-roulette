package com.cityroulette.service;

import com.cityroulette.domain.Place;
import com.cityroulette.dto.tourism.TourismPlaceDto;
import com.cityroulette.dto.tourism.TourismSyncResult;
import com.cityroulette.repository.PlaceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TourismImportServiceTest {

    @Autowired
    private TourismImportService importService;

    @Autowired
    private PlaceRepository placeRepository;

    @MockitoBean
    private TourismService tourismService;

    @Test
    void insertsNewPlaceWithUnknownIndoor() {
        when(tourismService.getBusanTouristPlaces(1, 10))
                .thenReturn(List.of(dto("1997221", "부산 감천문화마을", "129.01", "35.09")));

        TourismSyncResult result = importService.syncBusanTouristPlaces(1, 10);

        Place saved = placeRepository.findByExternalId("1997221").orElseThrow();
        assertThat(saved.getName()).isEqualTo("부산 감천문화마을");
        assertThat(saved.getIndoor()).isNull();
        assertThat(result).isEqualTo(new TourismSyncResult(1, 1, 0, 0));
        verify(tourismService).getBusanTouristPlaces(1, 10);
    }

    @Test
    void secondSynchronizationUpdatesPlaceWithoutAddingAnotherRow() {
        when(tourismService.getBusanTouristPlaces(1, 10))
                .thenReturn(List.of(dto("1997221", "부산 감천문화마을", "129.00", "35.00")))
                .thenReturn(List.of(dto("1997221", "감천문화마을", "129.01", "35.09")));

        TourismSyncResult firstResult = importService.syncBusanTouristPlaces(1, 10);
        Long insertedId = placeRepository.findByExternalId("1997221").orElseThrow().getId();
        TourismSyncResult secondResult = importService.syncBusanTouristPlaces(1, 10);

        Place updated = placeRepository.findByExternalId("1997221").orElseThrow();
        assertThat(placeRepository.count()).isEqualTo(1);
        assertThat(updated.getId()).isEqualTo(insertedId);
        assertThat(updated.getName()).isEqualTo("감천문화마을");
        assertThat(updated.getCategory()).isEqualTo("관광지");
        assertThat(updated.getLatitude()).isEqualTo(35.09);
        assertThat(updated.getLongitude()).isEqualTo(129.01);
        assertThat(firstResult).isEqualTo(new TourismSyncResult(1, 1, 0, 0));
        assertThat(secondResult).isEqualTo(new TourismSyncResult(1, 0, 1, 0));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void preservesManuallyManagedIndoorValue(Boolean indoor) {
        placeRepository.saveAndFlush(new Place(
                "1997221", "기존 이름", "관광지", "기존 주소", 35.0, 129.0, indoor));
        when(tourismService.getBusanTouristPlaces(1, 10))
                .thenReturn(List.of(dto("1997221", "새 이름", "129.01", "35.09")));

        importService.syncBusanTouristPlaces(1, 10);

        assertThat(placeRepository.findByExternalId("1997221").orElseThrow().getIndoor())
                .isEqualTo(indoor);
    }

    @Test
    void skipsPlacesWithoutContentIdOrTitle() {
        when(tourismService.getBusanTouristPlaces(1, 10)).thenReturn(List.of(
                dto(" ", "이름 있음", "129.01", "35.09"),
                dto("2000000", "", "129.01", "35.09")));

        TourismSyncResult result = importService.syncBusanTouristPlaces(1, 10);

        assertThat(placeRepository.count()).isZero();
        assertThat(result).isEqualTo(new TourismSyncResult(2, 0, 0, 2));
    }

    @Test
    void savesPlaceWhenCoordinatesAreMissingOrInvalid() {
        when(tourismService.getBusanTouristPlaces(1, 10))
                .thenReturn(List.of(dto("1997221", "감천문화마을", "not-a-number", "")));

        importService.syncBusanTouristPlaces(1, 10);

        Place saved = placeRepository.findByExternalId("1997221").orElseThrow();
        assertThat(saved.getLatitude()).isNull();
        assertThat(saved.getLongitude()).isNull();
    }

    @Test
    void reportsInsertedUpdatedAndSkippedCounts() {
        placeRepository.saveAndFlush(new Place(
                "1997221", "기존 이름", "관광지", "기존 주소", null, null, true));
        when(tourismService.getBusanTouristPlaces(2, 3)).thenReturn(List.of(
                dto("1997221", "새 이름", "129.01", "35.09"),
                dto("2000000", "새 장소", "129.02", "35.10"),
                dto(null, "저장 불가", "129.03", "35.11")));

        TourismSyncResult result = importService.syncBusanTouristPlaces(2, 3);

        assertThat(result).isEqualTo(new TourismSyncResult(3, 1, 1, 1));
        assertThat(placeRepository.count()).isEqualTo(2);
    }

    private TourismPlaceDto dto(String contentId, String title, String mapx, String mapy) {
        return new TourismPlaceDto(
                contentId, "12", title, "부산광역시 사하구", "감내2로 203", mapx, mapy,
                "", "", "", "26", "380", "VE", "VE01", "VE010100",
                "", "49349", "", "");
    }
}
