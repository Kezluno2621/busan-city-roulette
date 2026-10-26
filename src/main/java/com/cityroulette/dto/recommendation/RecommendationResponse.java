package com.cityroulette.dto.recommendation;

public record RecommendationResponse(
        Boolean recommended,
        String message,
        RecommendationDto recommendation) {

    public static RecommendationResponse success(RecommendationDto recommendation) {
        return new RecommendationResponse(
                true, "추천 장소를 찾았습니다.", recommendation);
    }

    public static RecommendationResponse notFound() {
        return new RecommendationResponse(
                false,
                "주어진 시간 안에 방문 가능한 관광지를 찾지 못했습니다.",
                null);
    }
}
