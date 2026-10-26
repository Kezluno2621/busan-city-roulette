package com.cityroulette.controller;

import com.cityroulette.dto.recommendation.RecommendationResponse;
import com.cityroulette.service.RecommendationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {

    private static final int MIN_AVAILABLE_MINUTES = 60;
    private static final int MAX_AVAILABLE_MINUTES = 720;
    private static final int MIN_STAY_MINUTES = 30;
    private static final int MAX_STAY_MINUTES = 240;
    private static final int MAX_CANDIDATE_LIMIT = 5;

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping("/random")
    public RecommendationResponse recommendRandomPlace(
            @RequestParam(name = "lat") double latitude,
            @RequestParam(name = "lon") double longitude,
            @RequestParam(name = "availableMinutes") int availableMinutes,
            @RequestParam(name = "stayMinutes", defaultValue = "60") int stayMinutes,
            @RequestParam(name = "limit", defaultValue = "3") int candidateLimit) {
        validate(latitude, longitude, availableMinutes, stayMinutes, candidateLimit);
        return recommendationService.recommend(
                latitude,
                longitude,
                availableMinutes,
                stayMinutes,
                candidateLimit);
    }

    private void validate(
            double latitude,
            double longitude,
            int availableMinutes,
            int stayMinutes,
            int candidateLimit) {
        if (!Double.isFinite(latitude) || latitude < -90 || latitude > 90) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "lat은 -90~90 범위의 위도여야 합니다.");
        }
        if (!Double.isFinite(longitude) || longitude < -180 || longitude > 180) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "lon은 -180~180 범위의 경도여야 합니다.");
        }
        if (availableMinutes < MIN_AVAILABLE_MINUTES
                || availableMinutes > MAX_AVAILABLE_MINUTES) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "availableMinutes는 60 이상 720 이하여야 합니다.");
        }
        if (stayMinutes < MIN_STAY_MINUTES || stayMinutes > MAX_STAY_MINUTES) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "stayMinutes는 30 이상 240 이하여야 합니다.");
        }
        if (candidateLimit < 1 || candidateLimit > MAX_CANDIDATE_LIMIT) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "limit는 1 이상 5 이하여야 합니다.");
        }
    }
}
