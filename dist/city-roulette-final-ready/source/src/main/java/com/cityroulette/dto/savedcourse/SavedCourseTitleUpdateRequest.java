package com.cityroulette.dto.savedcourse;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record SavedCourseTitleUpdateRequest(
        @NotBlank @Size(max = 100) String title,
        @NotNull @PositiveOrZero Long version) { }
