package com.cityroulette.dto.recommendation;

import java.util.List;

public record PersonalizedRecommendationResponse(
        List<RecommendationResultDto> recommendations,
        int candidateCount,
        boolean insufficientCandidates,
        String message,
        List<String> dataLimitations) { }
