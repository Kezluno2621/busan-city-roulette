package com.cityroulette.domain.transport.controller;

import com.cityroulette.domain.transport.dto.BusPageResponse;
import com.cityroulette.domain.transport.dto.BusRouteResponse;
import com.cityroulette.domain.transport.dto.BusRouteStopResponse;
import com.cityroulette.domain.transport.dto.BusStopResponse;
import com.cityroulette.domain.transport.service.BusQueryService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 버스 정류소와 노선의 읽기 전용 REST API를 제공합니다. */
@RestController
@RequestMapping("/api/bus")
@Validated
public class BusController {

    private final BusQueryService service;

    public BusController(BusQueryService service) {
        this.service = service;
    }

    @GetMapping("/stops")
    public BusPageResponse<BusStopResponse> stops(
            @RequestParam(required = false) @Size(max = 100) String name,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return service.stops(name, page, size);
    }

    @GetMapping("/stops/{id}")
    public BusStopResponse stop(@PathVariable Long id) {
        return service.stop(id);
    }

    @GetMapping("/routes")
    public BusPageResponse<BusRouteResponse> routes(
            @RequestParam(required = false) @Size(max = 50) String number,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return service.routes(number, page, size);
    }

    @GetMapping("/routes/{id}")
    public BusRouteResponse route(@PathVariable Long id) {
        return service.route(id);
    }

    @GetMapping("/routes/{id}/stops")
    public List<BusRouteStopResponse> routeStops(@PathVariable Long id) {
        return service.routeStops(id);
    }

    @GetMapping("/stops/{id}/routes")
    public List<BusRouteResponse> stopRoutes(@PathVariable Long id) {
        return service.stopRoutes(id);
    }
}
