package com.cityroulette.controller;

import com.cityroulette.dto.place.ApiErrorResponse;
import com.cityroulette.service.PlaceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;

@RestControllerAdvice(assignableTypes = PlaceController.class)
public class PlaceApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(PlaceApiExceptionHandler.class);

    @ExceptionHandler(PlaceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> notFound(
            PlaceNotFoundException exception,
            HttpServletRequest request) {
        return response(HttpStatus.NOT_FOUND, exception.getMessage(), request);
    }

    @ExceptionHandler({
            ConstraintViolationException.class,
            MethodArgumentTypeMismatchException.class,
            MethodArgumentNotValidException.class,
            IllegalArgumentException.class
    })
    public ResponseEntity<ApiErrorResponse> badRequest(
            Exception exception,
            HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, "요청 파라미터를 확인해주세요.", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> internalError(
            Exception exception,
            HttpServletRequest request) {
        log.error("Place API 처리 중 오류가 발생했습니다.", exception);
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "장소 정보를 처리하지 못했습니다.", request);
    }

    private ResponseEntity<ApiErrorResponse> response(
            HttpStatus status,
            String message,
            HttpServletRequest request) {
        return ResponseEntity.status(status).body(new ApiErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI()));
    }
}
