package com.cityroulette.controller;

import com.cityroulette.dto.bus.TransitRoutePlanResponse;
import com.cityroulette.service.TransitRoutePlannerService;
import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transit")
@Validated
public class TransitRouteController {
    private final TransitRoutePlannerService planner;
    public TransitRouteController(TransitRoutePlannerService planner){this.planner=planner;}

    @GetMapping(value="/routes",params={"originPlaceId","!sx","!sy","!ex","!ey"})
    public TransitRoutePlanResponse routes(
            @RequestParam @Positive Long originPlaceId,
            @RequestParam @Positive Long destinationPlaceId,
            @RequestParam(defaultValue="1000") @Min(50) @Max(3000) int radius,
            @RequestParam(defaultValue="10") @Min(1) @Max(20) int limit){
        return planner.plan(originPlaceId,destinationPlaceId,radius,limit);
    }
}
