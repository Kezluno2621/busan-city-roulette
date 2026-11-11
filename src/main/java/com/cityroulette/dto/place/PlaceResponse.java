package com.cityroulette.dto.place;

import com.cityroulette.domain.Place;

import java.time.LocalDateTime;

public record PlaceResponse(
        Long id,
        String name,
        String category,
        String address,
        String district,
        Double latitude,
        Double longitude,
        String description,
        String source,
        String externalId,
        Boolean indoor,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String imageUrl) {

    public static PlaceResponse from(Place place) {
        return new PlaceResponse(
                place.getId(),
                place.getName(),
                place.getCategory(),
                place.getAddress(),
                place.getDistrict(),
                place.getLatitude(),
                place.getLongitude(),
                place.getDescription(),
                place.getSource(),
                place.getExternalId(),
                place.getIndoor(),
                place.getCreatedAt(),
                place.getUpdatedAt(),
                place.getImageUrl());
    }
}
