package com.cityroulette.dto.recommendation;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 호출자가 보내는 입력의 계약입니다. 값은 선언된 필드와 검증 규칙에 따라 해석합니다.
 */
public record RecommendationRequest(
        Long originPlaceId,
        @DecimalMin("-90.0") @DecimalMax("90.0") Double originLatitude,
        @DecimalMin("-180.0") @DecimalMax("180.0") Double originLongitude,
        @Size(max = 10) List<@Size(max = 100) String> categories,
        @Size(max = 50) String district,
        @Positive Integer availableMinutes,
        @PositiveOrZero Long budget,
        @Min(0) @Max(100) Integer randomness,
        @Min(1) @Max(20) Integer limit,
        Long randomSeed,
        CategorySelectionMode categoryMode) {

    /** 기존 API 호출 코드와의 호환성을 유지하며 기본 모드는 선호 카테고리다. */
    public RecommendationRequest(
            Long originPlaceId,
            Double originLatitude,
            Double originLongitude,
            List<String> categories,
            String district,
            Integer availableMinutes,
            Long budget,
            Integer randomness,
            Integer limit,
            Long randomSeed) {
        this(originPlaceId, originLatitude, originLongitude, categories, district,
                availableMinutes, budget, randomness, limit, randomSeed,
                CategorySelectionMode.PREFERRED);
    }
}
