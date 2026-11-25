package com.cityroulette.domain.recommendation.dto;

public record LocationSearchResultDto(
        String id,
        String name,
        String category,
        String address,
        double latitude,
        double longitude) {
}
