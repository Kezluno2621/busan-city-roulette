package com.cityroulette.domain.course.controller;

import com.cityroulette.domain.course.service.InvalidGeneratedCourseException;
import com.cityroulette.domain.course.service.SavedCourseConflictException;
import com.cityroulette.domain.course.service.SavedCourseNotFoundException;
import jakarta.validation.ConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

/** 저장 코스 API의 조회·저장 오류를 응답으로 변환합니다. */
@RestControllerAdvice(assignableTypes = SavedCourseController.class)
public class SavedCourseApiExceptionHandler {
    @ExceptionHandler(SavedCourseNotFoundException.class)
    ResponseEntity<Map<String,Object>> notFound(RuntimeException exception){return error(HttpStatus.NOT_FOUND,exception.getMessage());}
    @ExceptionHandler(SavedCourseConflictException.class)
    ResponseEntity<Map<String,Object>> conflict(RuntimeException exception){return error(HttpStatus.CONFLICT,exception.getMessage());}
    @ExceptionHandler(InvalidGeneratedCourseException.class)
    ResponseEntity<Map<String,Object>> invalidToken(RuntimeException exception){return error(HttpStatus.BAD_REQUEST,exception.getMessage());}
    @ExceptionHandler({IllegalArgumentException.class,MethodArgumentNotValidException.class,ConstraintViolationException.class})
    ResponseEntity<Map<String,Object>> badRequest(Exception exception){return error(HttpStatus.BAD_REQUEST,"저장 코스 요청값을 확인해주세요.");}
    private ResponseEntity<Map<String,Object>> error(HttpStatus status,String message){Map<String,Object> body=new LinkedHashMap<>();body.put("status",status.value());body.put("error",status.getReasonPhrase());body.put("message",message);return ResponseEntity.status(status).body(body);}
}
