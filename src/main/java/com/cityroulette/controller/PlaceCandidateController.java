package com.cityroulette.controller;

import com.cityroulette.dto.place.NearbyPlaceDto;
import com.cityroulette.service.PlaceCandidateService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/places")
public class PlaceCandidateController {

    private static final int MAX_LIMIT = 10;

    private final PlaceCandidateService placeCandidateService;

    public PlaceCandidateController(PlaceCandidateService placeCandidateService) {
        this.placeCandidateService = placeCandidateService;
    }

    @GetMapping("/nearby")
    public List<NearbyPlaceDto> getNearbyPlaces(
            @RequestParam(name = "lat") double latitude,
            @RequestParam(name = "lon") double longitude,
            @RequestParam(name = "limit", defaultValue = "5") int limit) {
        validate(latitude, longitude, limit);
        return placeCandidateService.findNearestPlaces(latitude, longitude, limit);
    }

    private void validate(double latitude, double longitude, int limit) {
        if (!Double.isFinite(latitude) || latitude < -90 || latitude > 90) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "lat은 -90~90 범위의 위도여야 합니다.");
        }
        if (!Double.isFinite(longitude) || longitude < -180 || longitude > 180) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "lon은 -180~180 범위의 경도여야 합니다.");
        }
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "limit는 1 이상 10 이하여야 합니다.");
        }
    }
}
