package com.cityroulette.domain.course.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import com.cityroulette.domain.recommendation.dto.CategorySelectionMode;
import java.util.List;

/**
 * 룰렛 코스 생성 입력입니다. 선택 모드, 출발지, 장소 개수와 랜덤 시드 등 서버가 해석할 조건을 전달합니다.
 */
public record CourseGenerationRequest(
        Long originPlaceId,
        @DecimalMin("-90.0") @DecimalMax("90.0") Double originLatitude,
        @DecimalMin("-180.0") @DecimalMax("180.0") Double originLongitude,
        @Size(max = 10) List<@Size(max = 100) String> categories,
        @Size(max = 50) String district,
        @Positive Integer availableMinutes,
        @PositiveOrZero Long budget,
        @Min(0) @Max(100) Integer randomness,
        @Min(1) @Max(5) Integer placeCount,
        Long randomSeed,
        CategorySelectionMode categoryMode,
        CourseGenerationMode generationMode,
        @Size(max = 50) List<Long> excludedPlaceIds) {

    /** 기존 테스트와 API 호출부가 사용하던 10개 인자 생성자를 유지한다. */
    public CourseGenerationRequest(
            Long originPlaceId,
            Double originLatitude,
            Double originLongitude,
            List<String> categories,
            String district,
            Integer availableMinutes,
            Long budget,
            Integer randomness,
            Integer placeCount,
            Long randomSeed) {
        this(originPlaceId, originLatitude, originLongitude, categories, district,
                availableMinutes, budget, randomness, placeCount, randomSeed,
                CategorySelectionMode.PREFERRED, CourseGenerationMode.PERSONALIZED, List.of());
    }
}
