package com.cityroulette.domain.recommendation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** 추천 입력 범위, 후보 수, 교통 조회와 코스 정책을 한곳에 모은 불변 설정입니다. */
@ConfigurationProperties(prefix = "recommendation")
public record RecommendationSettings(
        @DefaultValue Defaults defaults,
        @DefaultValue Limits limits,
        @DefaultValue Transit transit,
        @DefaultValue Origin origin,
        @DefaultValue Course course) {

    public record Defaults(
            @DefaultValue("180") int availableMinutes,
            @DefaultValue("60") int stayMinutes,
            @DefaultValue("1") int uiCandidateLimit,
            @DefaultValue("10") int recommendationLimit) { }

    public record Limits(
            @DefaultValue("60") int minAvailableMinutes,
            @DefaultValue("720") int maxAvailableMinutes,
            @DefaultValue("30") int minStayMinutes,
            @DefaultValue("240") int maxStayMinutes,
            @DefaultValue("5") int maxCandidates,
            @DefaultValue("3") int uiMaxCandidates,
            @DefaultValue("200") int candidatePool,
            @DefaultValue("50000") double maxDistanceMeters) { }

    public record Transit(
            @DefaultValue("1000") int radiusMeters,
            @DefaultValue("10") int evaluationLimit) { }

    public record Origin(
            @DefaultValue("35.1796") double latitude,
            @DefaultValue("129.0756") double longitude) { }

    public record Course(
            @DefaultValue("200") int recommendationPoolLimit,
            @DefaultValue("3") int defaultPlaceCount,
            @DefaultValue("100") int instantRandomness,
            @DefaultValue("40") int personalizedRandomness,
            @DefaultValue("12.0") double categoryNoveltyBonus,
            @DefaultValue("1.35") double maxDiversityDistanceFactor,
            @DefaultValue("1000") double maxDiversityDistanceAllowanceMeters) { }
}
