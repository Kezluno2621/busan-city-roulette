package com.cityroulette.dto.place;

public record NearbyPlaceDto(
        Long id,
        String externalId,
        String name,
        String category,
        String address,
        Double latitude,
        Double longitude,
        Double distanceKm) {
}
