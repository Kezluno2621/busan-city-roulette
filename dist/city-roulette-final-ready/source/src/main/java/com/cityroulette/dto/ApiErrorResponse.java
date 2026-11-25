package com.cityroulette.dto;

import java.time.LocalDateTime;

/**
 * 특정 도메인에 종속되지 않는 REST API 공통 오류 응답이다.
 */
public record ApiErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path) {
}
