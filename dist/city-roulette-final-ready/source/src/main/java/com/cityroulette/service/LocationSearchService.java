package com.cityroulette.service;

import com.cityroulette.dto.location.KakaoPlaceDocument;
import com.cityroulette.dto.location.LocationSearchResultDto;
import com.cityroulette.repository.PlaceRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class LocationSearchService {

    private static final int SEARCH_RESULT_SIZE = 10;

    private final KakaoLocalApiClient kakaoLocalApiClient;
    private final PlaceRepository places;
    private final boolean localOnly;

    public LocationSearchService(KakaoLocalApiClient kakaoLocalApiClient, PlaceRepository places,
            @Value("${location.search.local-only:false}") boolean localOnly) {
        this.kakaoLocalApiClient = kakaoLocalApiClient;
        this.places = places;
        this.localOnly = localOnly;
    }

    public List<LocationSearchResultDto> search(String query) {
        if (localOnly || !kakaoLocalApiClient.isConfigured()) {
            return searchSavedPlaces(query.trim());
        }
        return kakaoLocalApiClient.searchPlaces(query.trim(), SEARCH_RESULT_SIZE)
                .documents()
                .stream()
                .map(this::toResult)
                .filter(Objects::nonNull)
                .toList();
    }

    private List<LocationSearchResultDto> searchSavedPlaces(String query) {
        return places.search(null, null, query,
                        PageRequest.of(0, SEARCH_RESULT_SIZE, Sort.by("name").ascending()))
                .stream()
                .filter(place -> place.getLatitude() != null && place.getLongitude() != null)
                .map(place -> new LocationSearchResultDto("PLACE-" + place.getId(), place.getName(),
                        place.getCategory(), place.getAddress(), place.getLatitude(), place.getLongitude()))
                .toList();
    }

    private LocationSearchResultDto toResult(KakaoPlaceDocument document) {
        Double longitude = parseCoordinate(document.x());
        Double latitude = parseCoordinate(document.y());
        if (longitude == null || latitude == null
                || longitude < -180 || longitude > 180
                || latitude < -90 || latitude > 90) {
            return null;
        }

        String address = document.roadAddressName();
        if (address == null || address.isBlank()) {
            address = document.addressName();
        }
        return new LocationSearchResultDto(
                document.id(),
                document.placeName(),
                document.categoryName(),
                address,
                latitude,
                longitude);
    }

    private Double parseCoordinate(String value) {
        try {
            double coordinate = Double.parseDouble(value);
            return Double.isFinite(coordinate) ? coordinate : null;
        } catch (NullPointerException | NumberFormatException exception) {
            return null;
        }
    }
}
