package com.cityroulette.domain.course.dto;

import com.cityroulette.domain.recommendation.dto.RecommendationTransitStatus;
import java.util.List;

public record CoursePlaceDto(
        Long placeId,
        String placeName,
        String category,
        String address,
        Double latitude,
        Double longitude,
        int sequence,
        double recommendationScore,
        List<String> recommendationReasons,
        double distanceFromPreviousMeters,
        RecommendationTransitStatus transitVerificationStatus,
        CourseEstimateStatus estimatedTimeStatus,
        CourseEstimateStatus estimatedCostStatus,
        List<String> dataLimitations,
        String imageUrl) {
    public CoursePlaceDto(Long placeId, String placeName, String category, String address,
            Double latitude, Double longitude, int sequence, double recommendationScore,
            List<String> recommendationReasons, double distanceFromPreviousMeters,
            RecommendationTransitStatus transitVerificationStatus, CourseEstimateStatus estimatedTimeStatus,
            CourseEstimateStatus estimatedCostStatus, List<String> dataLimitations) {
        this(placeId, placeName, category, address, latitude, longitude, sequence, recommendationScore,
                recommendationReasons, distanceFromPreviousMeters, transitVerificationStatus,
                estimatedTimeStatus, estimatedCostStatus, dataLimitations, null);
    }
}
