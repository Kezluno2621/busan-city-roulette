package com.cityroulette.dto.location;

public record LocationSearchResultDto(
        String id,
        String name,
        String category,
        String address,
        double latitude,
        double longitude) {
}
