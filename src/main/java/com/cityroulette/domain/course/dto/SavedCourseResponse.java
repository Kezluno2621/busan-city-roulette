package com.cityroulette.domain.course.dto;

import java.time.LocalDateTime;
import java.util.List;

public record SavedCourseResponse(Long id, String title, String originName, double originLatitude,
        double originLongitude, int requestedPlaceCount, int actualPlaceCount, int randomness,
        Integer availableMinutes, Long budget, CourseEstimateStatus estimateTimeStatus,
        CourseEstimateStatus estimateCostStatus, List<String> dataLimitations,
        List<SavedCoursePlaceResponse> places, LocalDateTime createdAt, LocalDateTime updatedAt, Long version) { }
