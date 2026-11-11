package com.cityroulette.controller;

import com.cityroulette.dto.bus.NearbyBusStopResponse;
import com.cityroulette.service.BusQueryService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 장소 좌표를 기준으로 주변 정류소를 검색하는 REST API입니다. */
@RestController
@RequestMapping("/api/places")
@Validated
public class NearbyBusStopController {

    private final BusQueryService service;

    public NearbyBusStopController(BusQueryService service) {
        this.service = service;
    }

    @GetMapping("/{placeId}/nearby-bus-stops")
    public NearbyBusStopResponse nearby(
            @PathVariable Long placeId,
            @RequestParam(defaultValue = "500") @Min(50) @Max(3000) int radius) {
        return service.nearby(placeId, radius);
    }
}
