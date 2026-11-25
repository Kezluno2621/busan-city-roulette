package com.cityroulette.domain.transport.controller;

import com.cityroulette.domain.transport.dto.TransitRoutePlanResponse;
import com.cityroulette.domain.transport.service.TransitRoutePlannerService;
import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/** 장소 ID를 기준으로 적재된 부산 버스의 경로 후보를 조회합니다. */
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
