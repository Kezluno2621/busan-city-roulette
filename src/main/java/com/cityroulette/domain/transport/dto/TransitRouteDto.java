package com.cityroulette.domain.transport.dto;

public record TransitRouteDto(
        Integer pathType,
        Integer totalTime,
        Integer totalWalk,
        Integer payment,
        Integer busTransitCount,
        Integer subwayTransitCount,
        String firstStartStation,
        String lastEndStation) {
}
