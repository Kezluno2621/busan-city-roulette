package com.cityroulette.service;

import com.cityroulette.dto.transit.OdsayPathDto;
import com.cityroulette.dto.transit.OdsayPathInfoDto;
import com.cityroulette.dto.transit.OdsayResponse;
import com.cityroulette.dto.transit.OdsayResult;
import com.cityroulette.dto.transit.TransitRouteDto;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TransitServiceTest {

    private static final double SX = 129.0105969965;
    private static final double SY = 35.0974606978;
    private static final double EX = 129.123566;
    private static final double EY = 35.115449;

    private final OdsayApiClient client = mock(OdsayApiClient.class);
    private final TransitService service = new TransitService(client);

    @Test
    void convertsAllPathsToApplicationDtos() {
        when(client.searchPublicTransit(SX, SY, EX, EY))
                .thenReturn(responseWithTimes(82, 61, 82));

        List<TransitRouteDto> routes = service.findRoutes(SX, SY, EX, EY);

        assertThat(routes).extracting(TransitRouteDto::totalTime)
                .containsExactly(82, 61, 82);
        assertThat(routes.get(0).totalWalk()).isEqualTo(1000);
        assertThat(routes.get(0).payment()).isEqualTo(1550);
        assertThat(routes.get(0).firstStartStation()).isEqualTo("출발 정류장");
        assertThat(routes.get(0).lastEndStation()).isEqualTo("도착 정류장");
        verify(client).searchPublicTransit(SX, SY, EX, EY);
    }

    @Test
    void selectsSmallestTotalTimeInsteadOfFirstPath() {
        when(client.searchPublicTransit(SX, SY, EX, EY))
                .thenReturn(responseWithTimes(82, 61, 82));

        Optional<TransitRouteDto> fastest = service.findFastestRoute(SX, SY, EX, EY);

        assertThat(fastest).isPresent();
        assertThat(fastest.orElseThrow().totalTime()).isEqualTo(61);
        verify(client).searchPublicTransit(SX, SY, EX, EY);
    }

    @Test
    void returnsEmptyForMissingOrEmptyPaths() {
        when(client.searchPublicTransit(SX, SY, EX, EY))
                .thenReturn(new OdsayResponse(new OdsayResult(List.of()), null));

        assertThat(service.findFastestRoute(SX, SY, EX, EY)).isEmpty();
    }

    @Test
    void ignoresPathsWithoutTotalTimeWhenSelectingFastest() {
        OdsayPathDto missingInfo = new OdsayPathDto(2, null);
        OdsayPathDto missingTime = new OdsayPathDto(3,
                new OdsayPathInfoDto(null, 100, 1550, 1, 1, "출발", "도착"));
        when(client.searchPublicTransit(SX, SY, EX, EY))
                .thenReturn(new OdsayResponse(
                        new OdsayResult(List.of(missingInfo, missingTime)), null));

        assertThat(service.findFastestRoute(SX, SY, EX, EY)).isEmpty();
    }

    @Test
    void returnsEmptyListWhenResultIsMissing() {
        when(client.searchPublicTransit(SX, SY, EX, EY))
                .thenReturn(new OdsayResponse(null, null));

        assertThat(service.findRoutes(SX, SY, EX, EY)).isEmpty();
    }

    private OdsayResponse responseWithTimes(Integer... times) {
        List<OdsayPathDto> paths = java.util.Arrays.stream(times)
                .map(time -> new OdsayPathDto(2,
                        new OdsayPathInfoDto(
                                time, 1000, 1550, 1, 0,
                                "출발 정류장", "도착 정류장")))
                .toList();
        return new OdsayResponse(new OdsayResult(paths), null);
    }
}
