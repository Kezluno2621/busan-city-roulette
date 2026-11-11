package com.cityroulette.dto.savedcourse;

import com.cityroulette.dto.recommendation.RecommendationTransitStatus;
import java.util.List;

public record SavedCoursePlaceResponse(Long placeId, int visitOrder, String placeName, String category,
        String address, double latitude, double longitude, double recommendationScore,
        List<String> recommendationReasons, double distanceFromPreviousMeters,
        RecommendationTransitStatus transitVerificationStatus, List<String> dataLimitations,
        String imageUrl) {
    public SavedCoursePlaceResponse(Long placeId, int visitOrder, String placeName, String category,
            String address, double latitude, double longitude, double recommendationScore,
            List<String> recommendationReasons, double distanceFromPreviousMeters,
            RecommendationTransitStatus transitVerificationStatus, List<String> dataLimitations) {
        this(placeId, visitOrder, placeName, category, address, latitude, longitude, recommendationScore,
                recommendationReasons, distanceFromPreviousMeters, transitVerificationStatus,
                dataLimitations, null);
    }
}
