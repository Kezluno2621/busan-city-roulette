package com.cityroulette.dto.recommendation;

public record RecommendationDto(
        Long placeId,
        String externalId,
        String name,
        String category,
        String address,
        Double latitude,
        Double longitude,
        Double distanceKm,
        Integer oneWayTransitMinutes,
        Integer estimatedRoundTripMinutes,
        Integer stayMinutes,
        Integer estimatedRequiredMinutes,
        Integer availableMinutes,
        Integer totalWalk,
        Integer payment,
        Integer busTransitCount,
        Integer subwayTransitCount,
        String firstStartStation,
        String lastEndStation,
        Integer pathType) {
}
