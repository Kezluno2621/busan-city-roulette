package com.cityroulette.controller;

import com.cityroulette.dto.ApiErrorResponse;
import com.cityroulette.service.BusResourceNotFoundException;
import com.cityroulette.service.PlaceCoordinatesMissingException;
import com.cityroulette.service.PlaceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** 버스·주변 정류소·정적 경로 API의 예외를 공통 오류 DTO로 변환합니다. */
@RestControllerAdvice(assignableTypes = {
        BusController.class,
        NearbyBusStopController.class,
        TransitRouteController.class
})
public class BusApiExceptionHandler {

    @ExceptionHandler({BusResourceNotFoundException.class, PlaceNotFoundException.class})
    ResponseEntity<ApiErrorResponse> missing(RuntimeException exception, HttpServletRequest request) {
        return response(HttpStatus.NOT_FOUND, exception.getMessage(), request);
    }

    @ExceptionHandler(PlaceCoordinatesMissingException.class)
    ResponseEntity<ApiErrorResponse> noCoordinates(
            PlaceCoordinatesMissingException exception,
            HttpServletRequest request) {
        return response(HttpStatus.UNPROCESSABLE_ENTITY, exception.getMessage(), request);
    }

    @ExceptionHandler({ConstraintViolationException.class, IllegalArgumentException.class})
    ResponseEntity<ApiErrorResponse> bad(Exception exception, HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, "요청 파라미터를 확인해주세요.", request);
    }

    private ResponseEntity<ApiErrorResponse> response(
            HttpStatus status,
            String message,
            HttpServletRequest request) {
        ApiErrorResponse body = new ApiErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }
}
