package com.cityroulette.dto.bus;

import java.util.List;

public record TransitRouteGraph(
        Long routeId,
        String externalRouteId,
        String routeNumber,
        List<TransitGraphStop> stops,
        boolean sequenceContinuous,
        boolean allStopsLocated,
        boolean repeatedStops) {
}
