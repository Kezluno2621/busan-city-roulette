package com.cityroulette.dto.course;

import java.time.Instant;
import java.util.List;

/**
 * 서버가 만든 방문 순서와 장소 목록, 알려진 데이터 한계를 클라이언트에 전달합니다.
 */
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
