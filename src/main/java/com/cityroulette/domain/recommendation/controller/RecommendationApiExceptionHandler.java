package com.cityroulette.domain.recommendation.controller;

import com.cityroulette.domain.place.service.PlaceCoordinatesMissingException;
import com.cityroulette.domain.place.service.PlaceNotFoundException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** 추천 API의 요청과 계산 오류를 응답으로 변환합니다. */
@RestControllerAdvice(assignableTypes = PersonalizedRecommendationController.class)
public class RecommendationApiExceptionHandler {
    @ExceptionHandler(PlaceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> notFound(PlaceNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(PlaceCoordinatesMissingException.class)
    public ResponseEntity<Map<String, Object>> missingCoordinates(PlaceCoordinatesMissingException exception) {
        return error(HttpStatus.UNPROCESSABLE_ENTITY, exception.getMessage());
    }

    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class})
    public ResponseEntity<Map<String, Object>> badRequest(Exception exception) {
        return error(HttpStatus.BAD_REQUEST, "추천 요청값을 확인해주세요.");
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
}
