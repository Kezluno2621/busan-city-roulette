package com.cityroulette.dto.place;

public record TransitPlaceCandidateDto(
        Long id,
        String externalId,
        String name,
        String category,
        String address,
        Double latitude,
        Double longitude,
        Double distanceKm,
        Integer totalTime,
        Integer totalWalk,
        Integer payment,
        Integer busTransitCount,
        Integer subwayTransitCount,
        String firstStartStation,
        String lastEndStation,
        Integer pathType,
        Boolean transitAvailable) {
}
