package com.cityroulette.dto.recommendation;

import java.util.List;

public record RecommendationResultDto(
        Long placeId,
        String placeName,
        String category,
        String address,
        Double latitude,
        Double longitude,
        double score,
        RecommendationScoreBreakdown scoreBreakdown,
        List<String> recommendationReasons,
        double distanceMeters,
        RecommendationTransitStatus transitVerificationStatus,
        List<String> dataLimitations,
        String imageUrl) {
    public RecommendationResultDto(Long placeId, String placeName, String category, String address,
            Double latitude, Double longitude, double score, RecommendationScoreBreakdown scoreBreakdown,
            List<String> recommendationReasons, double distanceMeters,
            RecommendationTransitStatus transitVerificationStatus, List<String> dataLimitations) {
        this(placeId, placeName, category, address, latitude, longitude, score, scoreBreakdown,
                recommendationReasons, distanceMeters, transitVerificationStatus, dataLimitations, null);
    }
}
