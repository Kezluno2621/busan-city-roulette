package com.cityroulette.controller;

import com.cityroulette.service.PlaceCoordinatesMissingException;
import com.cityroulette.service.PlaceNotFoundException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** 코스 생성 요청 오류를 API 오류 응답으로 변환합니다. */
@RestControllerAdvice(assignableTypes = CourseGenerationController.class)
public class CourseApiExceptionHandler {
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
        return error(HttpStatus.BAD_REQUEST, "코스 생성 요청값을 확인해주세요.");
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
}
