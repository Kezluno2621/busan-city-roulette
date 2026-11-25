package com.cityroulette.controller;

import com.cityroulette.dto.recommendation.PersonalizedRecommendationResponse;
import com.cityroulette.dto.recommendation.RecommendationRequest;
import com.cityroulette.service.PersonalizedRecommendationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 개인화 추천 POST 요청을 검증하고 계산을 Service에 위임한 뒤 추천 결과를 JSON으로 반환합니다.
 */
@RestController
@RequestMapping("/api/recommendations")
public class PersonalizedRecommendationController {
    private final PersonalizedRecommendationService recommendations;

    public PersonalizedRecommendationController(PersonalizedRecommendationService recommendations) {
        this.recommendations = recommendations;
    }

    @PostMapping
    public PersonalizedRecommendationResponse recommend(@Valid @RequestBody RecommendationRequest request) {
        return recommendations.recommend(request);
    }
}
