package com.cityroulette.service;

import com.cityroulette.config.RecommendationSettings;
import org.springframework.stereotype.Component;

/** 기존 추천 화면과 REST API가 같은 입력 범위를 사용하도록 검증 규칙을 공유합니다. */
@Component
public class RecommendationInputValidator {
    private final RecommendationSettings settings;

    public RecommendationInputValidator(RecommendationSettings settings) {
        this.settings = settings;
    }

    public ValidationError validate(Double latitude, Double longitude, Integer availableMinutes,
            Integer stayMinutes, Integer candidateLimit, boolean uiRequest) {
        if (latitude == null || !Double.isFinite(latitude) || latitude < -90 || latitude > 90) {
            return ValidationError.LATITUDE;
        }
        if (longitude == null || !Double.isFinite(longitude) || longitude < -180 || longitude > 180) {
            return ValidationError.LONGITUDE;
        }
        RecommendationSettings.Limits limits = settings.limits();
        if (availableMinutes == null || availableMinutes < limits.minAvailableMinutes()
                || availableMinutes > limits.maxAvailableMinutes()) {
            return ValidationError.AVAILABLE_MINUTES;
        }
        if (stayMinutes == null || stayMinutes < limits.minStayMinutes()
                || stayMinutes > limits.maxStayMinutes()) {
            return ValidationError.STAY_MINUTES;
        }
        int maximum = uiRequest ? limits.uiMaxCandidates() : limits.maxCandidates();
        if (candidateLimit == null || candidateLimit < 1 || candidateLimit > maximum) {
            return ValidationError.CANDIDATE_LIMIT;
        }
        return null;
    }

    public enum ValidationError {
        LATITUDE, LONGITUDE, AVAILABLE_MINUTES, STAY_MINUTES, CANDIDATE_LIMIT
    }
}
