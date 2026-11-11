package com.cityroulette.controller;

import com.cityroulette.service.*;
import jakarta.validation.ConstraintViolationException;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice(assignableTypes = PlaceFavoriteController.class)
public class PlaceFavoriteApiExceptionHandler {
    @ExceptionHandler(PlaceNotFoundException.class)
    ResponseEntity<Map<String,Object>> notFound(RuntimeException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("status", 404, "message", exception.getMessage()));
    }
    @ExceptionHandler(FavoriteAccessException.class)
    ResponseEntity<Map<String,Object>> access(RuntimeException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("status", 404, "message", exception.getMessage()));
    }
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<Map<String,Object>> duplicate() {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("status", 409, "message", "이미 즐겨찾기에 등록된 장소입니다."));
    }
    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class})
    ResponseEntity<Map<String,Object>> invalid(Exception exception) {
        return ResponseEntity.badRequest().body(Map.of("status", 400, "message", "즐겨찾기 요청값을 확인해주세요."));
    }
}
