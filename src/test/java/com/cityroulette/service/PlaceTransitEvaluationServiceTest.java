package com.cityroulette.service;

import com.cityroulette.dto.place.NearbyPlaceDto;
import com.cityroulette.dto.place.TransitPlaceCandidateDto;
import com.cityroulette.dto.transit.TransitRouteDto;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PlaceTransitEvaluationServiceTest {

    private static final double START_LATITUDE = 35.1160;
    private static final double START_LONGITUDE = 128.9680;

    private final PlaceCandidateService placeCandidateService =
            mock(PlaceCandidateService.class);
    private final TransitService transitService = mock(TransitService.class);
    private final PlaceTransitEvaluationService service =
            new PlaceTransitEvaluationService(placeCandidateService, transitService);

    @Test
    void evaluatesEachCandidateOnceAndSortsAvailableRoutesByTotalTime() {
        NearbyPlaceDto placeA = place(1L, "장소 A", 35.10, 129.01, 4.0);
        NearbyPlaceDto placeB = place(2L, "장소 B", 35.11, 129.02, 5.0);
        NearbyPlaceDto placeC = place(3L, "장소 C", 35.12, 129.03, 6.0);
        when(placeCandidateService.findNearestPlaces(START_LATITUDE, START_LONGITUDE, 3))
                .thenReturn(List.of(placeA, placeB, placeC));
        whenRoute(placeA, Optional.of(route(54, 900, 1550)));
        whenRoute(placeB, Optional.empty());
        whenRoute(placeC, Optional.of(route(31, 620, 2100)));

        List<TransitPlaceCandidateDto> result = service.evaluateCandidates(
                START_LATITUDE, START_LONGITUDE, 3);

        assertThat(result).extracting(TransitPlaceCandidateDto::name)
                .containsExactly("장소 C", "장소 A", "장소 B");
        assertThat(result).extracting(TransitPlaceCandidateDto::totalTime)
                .containsExactly(31, 54, null);
        assertThat(result).extracting(TransitPlaceCandidateDto::transitAvailable)
                .containsExactly(true, true, false);
        assertThat(result.get(0).totalWalk()).isEqualTo(620);
        assertThat(result.get(0).payment()).isEqualTo(2100);
        assertThat(result.get(0).pathType()).isEqualTo(3);

        verify(transitService, times(3)).findFastestRoute(
                org.mockito.ArgumentMatchers.anyDouble(),
                org.mockito.ArgumentMatchers.anyDouble(),
                org.mockito.ArgumentMatchers.anyDouble(),
                org.mockito.ArgumentMatchers.anyDouble());
        verifyRouteCalledOnce(placeA);
        verifyRouteCalledOnce(placeB);
        verifyRouteCalledOnce(placeC);
    }

    @Test
    void treatsRouteNotFoundErrorAsUnavailableAndContinues() {
        NearbyPlaceDto missing = place(1L, "경로 없는 장소", 35.10, 129.01, 4.0);
        NearbyPlaceDto available = place(2L, "경로 있는 장소", 35.11, 129.02, 5.0);
        when(placeCandidateService.findNearestPlaces(START_LATITUDE, START_LONGITUDE, 2))
                .thenReturn(List.of(missing, available));
        when(transitService.findFastestRoute(
                START_LONGITUDE, START_LATITUDE, missing.longitude(), missing.latitude()))
                .thenThrow(new OdsayApiException("검색 데이터 없음", "400"));
        whenRoute(available, Optional.of(route(42, 500, 1550)));

        List<TransitPlaceCandidateDto> result = service.evaluateCandidates(
                START_LATITUDE, START_LONGITUDE, 2);

        assertThat(result).extracting(TransitPlaceCandidateDto::name)
                .containsExactly("경로 있는 장소", "경로 없는 장소");
        assertThat(result.get(1).transitAvailable()).isFalse();
        assertThat(result.get(1).totalTime()).isNull();
        verifyRouteCalledOnce(missing);
        verifyRouteCalledOnce(available);
    }

    @Test
    void stopsOnAuthenticationOrServiceErrorToProtectCallQuota() {
        NearbyPlaceDto first = place(1L, "첫 장소", 35.10, 129.01, 4.0);
        NearbyPlaceDto second = place(2L, "두 번째 장소", 35.11, 129.02, 5.0);
        when(placeCandidateService.findNearestPlaces(START_LATITUDE, START_LONGITUDE, 2))
                .thenReturn(List.of(first, second));
        when(transitService.findFastestRoute(
                START_LONGITUDE, START_LATITUDE, first.longitude(), first.latitude()))
                .thenThrow(new OdsayApiException("ApiKey authentication failed", "500"));

        assertThatThrownBy(() -> service.evaluateCandidates(
                START_LATITUDE, START_LONGITUDE, 2))
                .isInstanceOf(OdsayApiException.class)
                .hasMessageContaining("authentication failed");

        verifyRouteCalledOnce(first);
        verify(transitService, never()).findFastestRoute(
                START_LONGITUDE, START_LATITUDE, second.longitude(), second.latitude());
    }

    @Test
    void returnsEmptyListWithoutTransitCallsWhenThereAreNoCandidates() {
        when(placeCandidateService.findNearestPlaces(START_LATITUDE, START_LONGITUDE, 3))
                .thenReturn(List.of());

        assertThat(service.evaluateCandidates(START_LATITUDE, START_LONGITUDE, 3))
                .isEmpty();
        verifyNoInteractions(transitService);
    }

    private NearbyPlaceDto place(
            Long id,
            String name,
            double latitude,
            double longitude,
            double distanceKm) {
        return new NearbyPlaceDto(
                id,
                "external-" + id,
                name,
                "관광지",
                "부산광역시",
                latitude,
                longitude,
                distanceKm);
    }

    private TransitRouteDto route(int totalTime, int totalWalk, int payment) {
        return new TransitRouteDto(
                3,
                totalTime,
                totalWalk,
                payment,
                1,
                1,
                "출발 정류장",
                "도착 정류장");
    }

    private void whenRoute(NearbyPlaceDto place, Optional<TransitRouteDto> route) {
        when(transitService.findFastestRoute(
                START_LONGITUDE, START_LATITUDE, place.longitude(), place.latitude()))
                .thenReturn(route);
    }

    private void verifyRouteCalledOnce(NearbyPlaceDto place) {
        verify(transitService).findFastestRoute(
                START_LONGITUDE, START_LATITUDE, place.longitude(), place.latitude());
    }
}
