package com.cityroulette.dto.course;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.List;

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
        Long randomSeed) { }
