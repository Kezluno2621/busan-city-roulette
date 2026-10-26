package com.cityroulette.service;

import com.cityroulette.domain.Place;
import com.cityroulette.dto.tourism.TourismPlaceDto;
import com.cityroulette.dto.tourism.TourismSyncResult;
import com.cityroulette.repository.PlaceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TourismImportService {

    private final TourismService tourismService;
    private final TourismPlaceMapper tourismPlaceMapper;
    private final PlaceRepository placeRepository;

    public TourismImportService(TourismService tourismService,
                                TourismPlaceMapper tourismPlaceMapper,
                                PlaceRepository placeRepository) {
        this.tourismService = tourismService;
        this.tourismPlaceMapper = tourismPlaceMapper;
        this.placeRepository = placeRepository;
    }

    @Transactional
    public TourismSyncResult syncBusanTouristPlaces(int page, int size) {
        List<TourismPlaceDto> places = tourismService.getBusanTouristPlaces(page, size);
        int inserted = 0;
        int updated = 0;
        int skipped = 0;

        for (TourismPlaceDto dto : places) {
            if (!hasRequiredFields(dto)) {
                skipped++;
                continue;
            }

            Place apiPlace = tourismPlaceMapper.toPlace(dto);
            Place existingPlace = placeRepository.findByExternalId(apiPlace.getExternalId())
                    .orElse(null);

            if (existingPlace == null) {
                placeRepository.save(apiPlace);
                inserted++;
                continue;
            }

            existingPlace.updateFromApi(
                    apiPlace.getName(),
                    apiPlace.getCategory(),
                    apiPlace.getAddress(),
                    apiPlace.getLatitude(),
                    apiPlace.getLongitude());
            updated++;
        }

        return new TourismSyncResult(places.size(), inserted, updated, skipped);
    }

    private boolean hasRequiredFields(TourismPlaceDto dto) {
        return dto != null
                && dto.contentid() != null
                && !dto.contentid().isBlank()
                && dto.title() != null
                && !dto.title().isBlank();
    }
}
