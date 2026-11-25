package com.cityroulette.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** 추천 점수의 다섯 평가 요소 가중치만 바인딩하는 불변 설정입니다. */
@ConfigurationProperties(prefix = "recommendation.weights")
public record RecommendationWeights(
        @DefaultValue("30") double category,
        @DefaultValue("30") double distance,
        @DefaultValue("15") double diversity,
        @DefaultValue("15") double dataQuality,
        @DefaultValue("10") double transit) { }
