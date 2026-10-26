package com.cityroulette.service;

import com.cityroulette.domain.Place;
import com.cityroulette.dto.place.NearbyPlaceDto;
import com.cityroulette.repository.PlaceRepository;
import com.cityroulette.util.DistanceCalculator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
public class PlaceCandidateService {

    private final PlaceRepository placeRepository;

    public PlaceCandidateService(PlaceRepository placeRepository) {
        this.placeRepository = placeRepository;
    }

    @Transactional(readOnly = true)
    public List<NearbyPlaceDto> findNearestPlaces(
            double startLatitude,
            double startLongitude,
            int limit) {
        return placeRepository.findByLatitudeIsNotNullAndLongitudeIsNotNull().stream()
                .filter(place -> place.getLatitude() != null && place.getLongitude() != null)
                .map(place -> toNearbyPlace(place, startLatitude, startLongitude))
                .sorted(Comparator.comparingDouble(NearbyPlaceDto::distanceKm))
                .limit(limit)
                .toList();
    }

    private NearbyPlaceDto toNearbyPlace(
            Place place,
            double startLatitude,
            double startLongitude) {
        double distanceKm = DistanceCalculator.calculateKm(
                startLatitude,
                startLongitude,
                place.getLatitude(),
                place.getLongitude());

        return new NearbyPlaceDto(
                place.getId(),
                place.getExternalId(),
                place.getName(),
                place.getCategory(),
                place.getAddress(),
                place.getLatitude(),
                place.getLongitude(),
                distanceKm);
    }
}
