package com.cityroulette.controller;
import com.cityroulette.dto.place.ApiErrorResponse;
import com.cityroulette.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
@RestControllerAdvice(assignableTypes={BusController.class,NearbyBusStopController.class,TransitRouteController.class})
public class BusApiExceptionHandler {
    @ExceptionHandler({BusResourceNotFoundException.class,PlaceNotFoundException.class}) ResponseEntity<ApiErrorResponse> missing(RuntimeException e,HttpServletRequest r){return response(HttpStatus.NOT_FOUND,e.getMessage(),r);}
    @ExceptionHandler(PlaceCoordinatesMissingException.class) ResponseEntity<ApiErrorResponse> noCoordinates(PlaceCoordinatesMissingException e,HttpServletRequest r){return response(HttpStatus.UNPROCESSABLE_ENTITY,e.getMessage(),r);}
    @ExceptionHandler({ConstraintViolationException.class,IllegalArgumentException.class}) ResponseEntity<ApiErrorResponse> bad(Exception e,HttpServletRequest r){return response(HttpStatus.BAD_REQUEST,"요청 파라미터를 확인해주세요.",r);}
    private ResponseEntity<ApiErrorResponse> response(HttpStatus s,String m,HttpServletRequest r){return ResponseEntity.status(s).body(new ApiErrorResponse(LocalDateTime.now(),s.value(),s.getReasonPhrase(),m,r.getRequestURI()));}
}
