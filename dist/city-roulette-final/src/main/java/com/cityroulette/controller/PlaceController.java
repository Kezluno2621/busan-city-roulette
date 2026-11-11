package com.cityroulette.controller;

import com.cityroulette.dto.place.PlacePageResponse;
import com.cityroulette.dto.place.PlaceResponse;
import com.cityroulette.service.PlaceService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/places")
@Validated
public class PlaceController {

    private final PlaceService placeService;

    public PlaceController(PlaceService placeService) {
        this.placeService = placeService;
    }

    @GetMapping
    public PlacePageResponse getPlaces(
            @RequestParam(name = "category", required = false) @Size(max = 100) String category,
            @RequestParam(name = "district", required = false) @Size(max = 50) String district,
            @RequestParam(name = "keyword", required = false) @Size(max = 100) String keyword,
            @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
            @RequestParam(name = "size", defaultValue = "20") @Min(1) @Max(100) int size) {
        return placeService.searchPlaces(category, district, keyword, page, size);
    }

    @GetMapping("/{id}")
    public PlaceResponse getPlace(@PathVariable Long id) {
        return placeService.getPlace(id);
    }
}
