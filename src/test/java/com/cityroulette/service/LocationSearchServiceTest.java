package com.cityroulette.service;

import com.cityroulette.dto.location.KakaoPlaceDocument;
import com.cityroulette.dto.location.KakaoPlaceSearchResponse;
import com.cityroulette.dto.location.LocationSearchResultDto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LocationSearchServiceTest {

    private final KakaoLocalApiClient client = mock(KakaoLocalApiClient.class);
    private final LocationSearchService service = new LocationSearchService(client);

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
}
