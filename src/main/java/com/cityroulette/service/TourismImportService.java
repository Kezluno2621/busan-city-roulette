package com.cityroulette.service;

import com.cityroulette.dto.tourism.TourismPlaceDto;
import com.cityroulette.dto.tourism.TourismImportPageResult;
import com.cityroulette.dto.tourism.TourismSyncResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TourismImportService {

    private final TourismService tourismService;
    private final TourismPageImporter pageImporter;

    public TourismImportService(TourismService tourismService,
                                TourismPageImporter pageImporter) {
        this.tourismService = tourismService;
        this.pageImporter = pageImporter;
    }

    @Transactional
    public TourismSyncResult syncBusanTouristPlaces(int page, int size) {
        List<TourismPlaceDto> places = tourismService.getBusanTouristPlaces(page, size);
        TourismImportPageResult result = pageImporter.importPage(places, false);
        return new TourismSyncResult(result.received(), result.inserted(), result.updated(),
                result.skipped() + result.failed());
    }
}
