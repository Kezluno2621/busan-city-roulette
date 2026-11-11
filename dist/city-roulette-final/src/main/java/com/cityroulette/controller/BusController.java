package com.cityroulette.controller;
import com.cityroulette.dto.bus.*;
import com.cityroulette.service.BusQueryService;
import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/api/bus") @Validated
public class BusController {
    private final BusQueryService service;public BusController(BusQueryService service){this.service=service;}
    @GetMapping("/stops") public BusPageResponse<BusStopResponse> stops(@RequestParam(required=false)@Size(max=100)String name,@RequestParam(defaultValue="0")@Min(0)int page,@RequestParam(defaultValue="20")@Min(1)@Max(100)int size){return service.stops(name,page,size);}
    @GetMapping("/stops/{id}") public BusStopResponse stop(@PathVariable Long id){return service.stop(id);}
    @GetMapping("/routes") public BusPageResponse<BusRouteResponse> routes(@RequestParam(required=false)@Size(max=50)String number,@RequestParam(defaultValue="0")@Min(0)int page,@RequestParam(defaultValue="20")@Min(1)@Max(100)int size){return service.routes(number,page,size);}
    @GetMapping("/routes/{id}") public BusRouteResponse route(@PathVariable Long id){return service.route(id);}
    @GetMapping("/routes/{id}/stops") public List<BusRouteStopResponse> routeStops(@PathVariable Long id){return service.routeStops(id);}
    @GetMapping("/stops/{id}/routes") public List<BusRouteResponse> stopRoutes(@PathVariable Long id){return service.stopRoutes(id);}
}
