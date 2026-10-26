package com.cityroulette.controller;

import com.cityroulette.dto.tourism.TourismPlaceDto;
import com.cityroulette.dto.tourism.TourismSyncResult;
import com.cityroulette.service.TourismImportService;
import com.cityroulette.service.TourismService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/tourism")
public class TourismController {

    private final TourismService tourismService;
    private final TourismImportService tourismImportService;

    public TourismController(TourismService tourismService,
                             TourismImportService tourismImportService) {
        this.tourismService = tourismService;
        this.tourismImportService = tourismImportService;
    }

    @GetMapping("/places")
    public List<TourismPlaceDto> getPlaces(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        validatePagination(page, size);
        return tourismService.getBusanTouristPlaces(page, size);
    }

    @PostMapping("/sync")
    public TourismSyncResult syncPlaces(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        validatePagination(page, size);
        return tourismImportService.syncBusanTouristPlaces(page, size);
    }

    private void validatePagination(int page, int size) {
        if (page < 1 || size < 1 || size > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "page는 1 이상, size는 1 이상 100 이하여야 합니다.");
        }
    }
}
