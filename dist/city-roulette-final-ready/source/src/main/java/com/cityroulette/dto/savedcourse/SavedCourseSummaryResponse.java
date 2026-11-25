package com.cityroulette.dto.savedcourse;

import java.time.LocalDateTime;

public record SavedCourseSummaryResponse(Long id, String title, int actualPlaceCount,
        String representativePlaceName, LocalDateTime createdAt, LocalDateTime updatedAt, Long version) { }
