package com.cityroulette.domain.course.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 호출자가 보내는 입력의 계약입니다. 값은 선언된 필드와 검증 규칙에 따라 해석합니다.
 */
public record SavedCourseCreateRequest(
        @NotBlank @Size(max = 100) String title,
        @NotBlank @Size(max = 100) String generationToken) { }
