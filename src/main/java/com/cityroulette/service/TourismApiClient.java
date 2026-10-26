package com.cityroulette.service;

import com.cityroulette.dto.tourism.TourismApiResponse;
import com.cityroulette.dto.tourism.TourismHeader;
import org.springframework.web.client.RestClient;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class TourismApiClient {

    private static final String SUCCESS_CODE = "0000";

    private final RestClient restClient;
    private final String serviceKey;
    private final String mobileOs;
    private final String mobileApp;

    public TourismApiClient(RestClient restClient, String serviceKey,
                            String mobileOs, String mobileApp) {
        this.restClient = restClient;
        this.serviceKey = serviceKey;
        this.mobileOs = mobileOs;
        this.mobileApp = mobileApp;
    }

    public TourismApiResponse getBusanTouristPlaces(int pageNo, int numOfRows) {
        String decodedServiceKey = decodeServiceKey(serviceKey);

        TourismApiResponse apiResponse = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/areaBasedList2")
                        .queryParam("serviceKey", "{serviceKey}")
                        .queryParam("numOfRows", numOfRows)
                        .queryParam("pageNo", pageNo)
                        .queryParam("MobileOS", mobileOs)
                        .queryParam("MobileApp", mobileApp)
                        .queryParam("_type", "json")
                        .queryParam("arrange", "C")
                        .queryParam("contentTypeId", "12")
                        .queryParam("lDongRegnCd", "26")
                        .build(decodedServiceKey))
                .retrieve()
                .body(TourismApiResponse.class);

        validateResponse(apiResponse);
        return apiResponse;
    }

    private String decodeServiceKey(String key) {
        if (key == null || key.isBlank()) {
            throw new TourismApiException("TOUR_API_KEY 환경변수가 설정되지 않았습니다.");
        }

        try {
            // URLDecoder가 디코딩된 키의 '+'를 공백으로 바꾸지 않도록 먼저 보호한다.
            return URLDecoder.decode(key.trim().replace("+", "%2B"), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException exception) {
            throw new TourismApiException("TOUR_API_KEY 형식이 올바르지 않습니다.");
        }
    }

    private void validateResponse(TourismApiResponse apiResponse) {
        if (apiResponse == null || apiResponse.response() == null
                || apiResponse.response().header() == null) {
            throw new TourismApiException("관광 API 응답 헤더가 없습니다.");
        }

        TourismHeader header = apiResponse.response().header();
        if (!SUCCESS_CODE.equals(header.resultCode())) {
            throw new TourismApiException(
                    "관광 API 오류: resultCode=" + header.resultCode()
                            + ", resultMsg=" + header.resultMsg());
        }
    }
}
