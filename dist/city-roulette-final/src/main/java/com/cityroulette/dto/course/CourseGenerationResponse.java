package com.cityroulette.dto.course;

import java.time.Instant;
import java.util.List;

public record CourseGenerationResponse(
        String courseTitle,
        CourseOriginDto origin,
        Instant generatedAt,
        int requestedPlaceCount,
        int actualPlaceCount,
        List<CoursePlaceDto> places,
        boolean insufficientCandidates,
        String message,
        List<String> dataLimitations,
        String saveToken) { }
