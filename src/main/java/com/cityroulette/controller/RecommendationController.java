package com.cityroulette.controller;

import com.cityroulette.dto.recommendation.RecommendationResponse;
import com.cityroulette.service.RecommendationService;
import com.cityroulette.service.RecommendationInputValidator;
import com.cityroulette.service.RecommendationInputValidator.ValidationError;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** 무작위 장소 추천 API를 제공합니다. */
@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final RecommendationInputValidator validator;

    public RecommendationController(RecommendationService recommendationService,
            RecommendationInputValidator validator) {
        this.recommendationService = recommendationService;
        this.validator = validator;
    }

    @GetMapping("/random")
    public RecommendationResponse recommendRandomPlace(
            @RequestParam(name = "lat") double latitude,
            @RequestParam(name = "lon") double longitude,
            @RequestParam(name = "availableMinutes") int availableMinutes,
            @RequestParam(name = "stayMinutes", defaultValue = "60") int stayMinutes,
            @RequestParam(name = "limit", defaultValue = "3") int candidateLimit) {
        ValidationError error = validator.validate(
                latitude, longitude, availableMinutes, stayMinutes, candidateLimit, false);
        if (error != null) throw badRequest(error);
        return recommendationService.recommend(
                latitude,
                longitude,
                availableMinutes,
                stayMinutes,
                candidateLimit);
    }

    private ResponseStatusException badRequest(ValidationError error) {
        String message = switch (error) {
            case LATITUDE -> "lat은 -90~90 범위의 위도여야 합니다.";
            case LONGITUDE -> "lon은 -180~180 범위의 경도여야 합니다.";
            case AVAILABLE_MINUTES -> "availableMinutes는 60 이상 720 이하여야 합니다.";
            case STAY_MINUTES -> "stayMinutes는 30 이상 240 이하여야 합니다.";
            case CANDIDATE_LIMIT -> "limit는 1 이상 5 이하여야 합니다.";
        };
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
