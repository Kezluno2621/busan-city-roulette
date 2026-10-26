package com.cityroulette.service;

import com.cityroulette.dto.tourism.TourismApiResponse;
import com.cityroulette.dto.tourism.TourismBody;
import com.cityroulette.dto.tourism.TourismItems;
import com.cityroulette.dto.tourism.TourismPlaceDto;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TourismService {

    private final TourismApiClient tourismApiClient;

    public TourismService(TourismApiClient tourismApiClient) {
        this.tourismApiClient = tourismApiClient;
    }

    public List<TourismPlaceDto> getBusanTouristPlaces(int pageNo, int numOfRows) {
        TourismApiResponse apiResponse = tourismApiClient.getBusanTouristPlaces(pageNo, numOfRows);

        if (apiResponse.response() == null) {
            return List.of();
        }

        TourismBody body = apiResponse.response().body();
        if (body == null) {
            return List.of();
        }

        TourismItems items = body.items();
        if (items == null || items.item() == null) {
            return List.of();
        }

        return List.copyOf(items.item());
    }
}
