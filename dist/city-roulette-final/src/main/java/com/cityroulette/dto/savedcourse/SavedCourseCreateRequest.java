package com.cityroulette.dto.savedcourse;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SavedCourseCreateRequest(
        @NotBlank @Size(max = 100) String title,
        @NotBlank @Size(max = 100) String generationToken) { }
