package com.cityroulette.controller;
import com.cityroulette.dto.bus.NearbyBusStopResponse;
import com.cityroulette.service.BusQueryService;
import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/places") @Validated
public class NearbyBusStopController {
    private final BusQueryService service;public NearbyBusStopController(BusQueryService service){this.service=service;}
    @GetMapping("/{placeId}/nearby-bus-stops") public NearbyBusStopResponse nearby(@PathVariable Long placeId,@RequestParam(defaultValue="500")@Min(50)@Max(3000)int radius){return service.nearby(placeId,radius);}
}
