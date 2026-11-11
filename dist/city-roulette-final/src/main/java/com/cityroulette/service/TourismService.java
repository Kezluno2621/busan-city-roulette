package com.cityroulette.service;

import com.cityroulette.dto.tourism.TourismApiResponse;
import com.cityroulette.dto.tourism.TourismBody;
import com.cityroulette.dto.tourism.TourismItems;
import com.cityroulette.dto.tourism.TourismPlaceDto;
import com.cityroulette.dto.tourism.TourismPage;
import com.cityroulette.dto.tourism.TourismRegionApiResponse;
import com.cityroulette.dto.tourism.TourismRegionDto;
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

    public TourismPage getBusanPlacesPage(int pageNo, int numOfRows) {
        TourismApiResponse apiResponse = tourismApiClient.getBusanPlaces(pageNo, numOfRows);
        if (apiResponse.response() == null || apiResponse.response().body() == null) {
            return new TourismPage(List.of(), pageNo, numOfRows, 0);
        }
        TourismBody body = apiResponse.response().body();
        List<TourismPlaceDto> items = body.items() == null || body.items().item() == null
                ? List.of() : List.copyOf(body.items().item());
        return new TourismPage(items, body.pageNo(), body.numOfRows(), body.totalCount());
    }

    public void verifyBusanLegalDongCode() {
        TourismRegionApiResponse response = tourismApiClient.getLegalDongRegions();
        if (response.response().body() == null || response.response().body().items() == null
                || response.response().body().items().item() == null) {
            throw new TourismApiException("TourAPI 법정동 코드 응답에 지역 목록이 없습니다.");
        }
        boolean verified = response.response().body().items().item().stream()
                .anyMatch(this::isBusanRegion);
        if (!verified) {
            throw new TourismApiException("TourAPI 법정동 코드 26(부산광역시)을 응답에서 확인하지 못했습니다.");
        }
    }

    private boolean isBusanRegion(TourismRegionDto region) {
        return region != null && "26".equals(region.lDongRegnCd())
                && region.lDongRegnNm() != null && region.lDongRegnNm().contains("부산");
    }
}
