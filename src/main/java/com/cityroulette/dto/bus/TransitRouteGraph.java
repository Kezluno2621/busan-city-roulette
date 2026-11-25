package com.cityroulette.dto.bus;

import java.util.List;

/**
 * 노선별로 순서가 있는 정차 항목을 전달합니다. 경로 탐색기는 각 정류장 등장 위치를 구분해 승차·하차 방향을 검증합니다.
 */
public record TransitRouteGraph(
        Long routeId,
        String externalRouteId,
        String routeNumber,
        List<TransitGraphStop> stops,
        boolean sequenceContinuous,
        boolean allStopsLocated,
        boolean repeatedStops) {
}
