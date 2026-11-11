package com.cityroulette.dto.recommendation;

public record RecommendationScoreBreakdown(
        double categoryPreference,
        double distance,
        double diversity,
        double dataQuality,
        double transitConnectivity,
        boolean transitIncluded) { }
