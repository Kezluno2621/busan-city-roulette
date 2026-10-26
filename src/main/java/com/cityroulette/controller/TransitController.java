package com.cityroulette.controller;

import com.cityroulette.dto.transit.TransitRouteDto;
import com.cityroulette.service.TransitService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/transit")
public class TransitController {

    private final TransitService transitService;

    public TransitController(TransitService transitService) {
        this.transitService = transitService;
    }

    @GetMapping("/routes")
    public List<TransitRouteDto> getRoutes(
            @RequestParam(name = "sx") double startLongitude,
            @RequestParam(name = "sy") double startLatitude,
            @RequestParam(name = "ex") double endLongitude,
            @RequestParam(name = "ey") double endLatitude) {
        validateCoordinates(startLongitude, startLatitude, endLongitude, endLatitude);
        return transitService.findRoutes(
                startLongitude, startLatitude, endLongitude, endLatitude);
    }

    @GetMapping("/fastest")
    public ResponseEntity<TransitRouteDto> getFastestRoute(
            @RequestParam(name = "sx") double startLongitude,
            @RequestParam(name = "sy") double startLatitude,
            @RequestParam(name = "ex") double endLongitude,
            @RequestParam(name = "ey") double endLatitude) {
        validateCoordinates(startLongitude, startLatitude, endLongitude, endLatitude);
        return ResponseEntity.of(transitService.findFastestRoute(
                startLongitude, startLatitude, endLongitude, endLatitude));
    }

    private void validateCoordinates(
            double startLongitude,
            double startLatitude,
            double endLongitude,
            double endLatitude) {
        if (!isLongitude(startLongitude) || !isLongitude(endLongitude)
                || !isLatitude(startLatitude) || !isLatitude(endLatitude)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "sx/ex는 -180~180의 경도, sy/ey는 -90~90의 위도여야 합니다.");
        }
    }

    private boolean isLongitude(double value) {
        return Double.isFinite(value) && value >= -180 && value <= 180;
    }

    private boolean isLatitude(double value) {
        return Double.isFinite(value) && value >= -90 && value <= 90;
    }
}
