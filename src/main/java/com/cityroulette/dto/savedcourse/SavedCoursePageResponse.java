package com.cityroulette.dto.savedcourse;

import java.util.List;

public record SavedCoursePageResponse(List<SavedCourseSummaryResponse> content, int page, int size,
        long totalElements, int totalPages) { }
