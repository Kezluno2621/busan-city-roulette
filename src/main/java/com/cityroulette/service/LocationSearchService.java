package com.cityroulette.service;

import com.cityroulette.dto.location.KakaoPlaceDocument;
import com.cityroulette.dto.location.LocationSearchResultDto;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class LocationSearchService {

    private static final int SEARCH_RESULT_SIZE = 10;

    private final KakaoLocalApiClient kakaoLocalApiClient;

    public LocationSearchService(KakaoLocalApiClient kakaoLocalApiClient) {
        this.kakaoLocalApiClient = kakaoLocalApiClient;
    }

    public List<LocationSearchResultDto> search(String query) {
        return kakaoLocalApiClient.searchPlaces(query.trim(), SEARCH_RESULT_SIZE)
                .documents()
                .stream()
                .map(this::toResult)
                .filter(Objects::nonNull)
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
