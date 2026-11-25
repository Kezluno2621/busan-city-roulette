package com.cityroulette.dto.recommendation;

/**
 * 최종 추천 점수에 사용한 항목별 결과와 평가 여부를 응답으로 전달합니다.
 */
public record RecommendationScoreBreakdown(
        double categoryPreference,
        double distance,
        double diversity,
        double dataQuality,
        double transitConnectivity,
        boolean transitIncluded) { }
