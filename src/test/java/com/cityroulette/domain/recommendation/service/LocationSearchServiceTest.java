package com.cityroulette.domain.recommendation.service;

import com.cityroulette.integration.kakao.KakaoLocalApiClient;
import com.cityroulette.integration.kakao.dto.KakaoPlaceDocument;
import com.cityroulette.integration.kakao.dto.KakaoPlaceSearchResponse;
import com.cityroulette.domain.recommendation.dto.LocationSearchResultDto;
import com.cityroulette.domain.place.entity.Place;
import com.cityroulette.domain.place.repository.PlaceRepository;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LocationSearchServiceTest {

    private final KakaoLocalApiClient client = mock(KakaoLocalApiClient.class);
    private final PlaceRepository places = mock(PlaceRepository.class);
    private final LocationSearchService service = new LocationSearchService(client, places, false);

    @BeforeEach
    void externalSearchIsConfigured() {
        when(client.isConfigured()).thenReturn(true);
    }

    @Test
    void mapsKakaoCoordinatesAndPrefersRoadAddress() {
        when(client.searchPlaces("부산역", 10)).thenReturn(new KakaoPlaceSearchResponse(List.of(
                new KakaoPlaceDocument(
                        "101", "부산역", "교통,수송 > 기차역",
                        "부산 동구 초량동", "부산 동구 중앙대로 206",
                        "129.0414", "35.1151"))));

        List<LocationSearchResultDto> results = service.search(" 부산역 ");

        assertThat(results).containsExactly(new LocationSearchResultDto(
                "101", "부산역", "교통,수송 > 기차역", "부산 동구 중앙대로 206",
                35.1151, 129.0414));
        verify(client).searchPlaces("부산역", 10);
    }

    @Test
    void skipsDocumentsWithInvalidCoordinates() {
        when(client.searchPlaces("잘못된 장소", 10)).thenReturn(new KakaoPlaceSearchResponse(List.of(
                new KakaoPlaceDocument(
                        "102", "잘못된 장소", "", "부산", "", "not-a-number", "35.1"))));

        assertThat(service.search("잘못된 장소")).isEmpty();
    }

    @Test
    void demoLocalOnlyModeSearchesSavedPlacesWithoutCallingKakao() {
        Place place = new Place("부산역 주변 명소", "관광지", "부산광역시 동구", 35.1151, 129.0414, false);
        when(places.search(org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.eq("부산역"), org.mockito.ArgumentMatchers.any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(place)));
        LocationSearchService local = new LocationSearchService(client, places, true);

        assertThat(local.search(" 부산역 ")).singleElement().satisfies(result -> {
            assertThat(result.name()).isEqualTo("부산역 주변 명소");
            assertThat(result.latitude()).isEqualTo(35.1151);
            assertThat(result.longitude()).isEqualTo(129.0414);
        });
        org.mockito.Mockito.verifyNoInteractions(client);
    }

    @Test
    void fallsBackToSavedPlacesWhenKakaoKeyIsMissing() {
        when(client.isConfigured()).thenReturn(false);
        Place place = new Place("부산역 주변 명소", "관광지", "부산광역시 동구", 35.1151, 129.0414, false);
        when(places.search(org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.eq("부산역"), org.mockito.ArgumentMatchers.any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(place)));

        assertThat(service.search("부산역")).singleElement()
                .extracting(LocationSearchResultDto::name)
                .isEqualTo("부산역 주변 명소");
    }
}
