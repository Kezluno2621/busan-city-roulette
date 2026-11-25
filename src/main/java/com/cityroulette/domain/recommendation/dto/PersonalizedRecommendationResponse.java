package com.cityroulette.domain.recommendation.dto;

import java.util.List;

public record PersonalizedRecommendationResponse(
        List<RecommendationResultDto> recommendations,
        int candidateCount,
        boolean insufficientCandidates,
        String message,
        List<String> dataLimitations) { }
