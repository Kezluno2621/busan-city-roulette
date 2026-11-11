package com.cityroulette.controller;

import com.cityroulette.dto.recommendation.PersonalizedRecommendationResponse;
import com.cityroulette.dto.recommendation.RecommendationRequest;
import com.cityroulette.service.PersonalizedRecommendationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
