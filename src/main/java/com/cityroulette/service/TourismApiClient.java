package com.cityroulette.service;

import com.cityroulette.dto.tourism.TourismApiResponse;
import com.cityroulette.dto.tourism.TourismHeader;
import com.cityroulette.dto.tourism.TourismRegionApiResponse;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

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
        return getBusanPlaces(pageNo, numOfRows, "12");
    }

    public boolean isConfigured() {
        return serviceKey != null && !serviceKey.isBlank();
    }

    public TourismApiResponse getBusanPlaces(int pageNo, int numOfRows) {
        return getBusanPlaces(pageNo, numOfRows, null);
    }

    private TourismApiResponse getBusanPlaces(int pageNo, int numOfRows, String contentTypeId) {
        String decodedServiceKey = decodeServiceKey(serviceKey);

        TourismApiResponse apiResponse = executeWithRetry(() -> restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/areaBasedList2")
                        .queryParam("serviceKey", "{serviceKey}")
                        .queryParam("numOfRows", numOfRows)
                        .queryParam("pageNo", pageNo)
                        .queryParam("MobileOS", mobileOs)
                        .queryParam("MobileApp", mobileApp)
                        .queryParam("_type", "json")
                        .queryParam("arrange", "C")
                        .queryParamIfPresent("contentTypeId", java.util.Optional.ofNullable(contentTypeId))
                        .queryParam("lDongRegnCd", "26")
                        .build(decodedServiceKey))
                .retrieve()
                .body(TourismApiResponse.class), "관광 API");

        validateResponse(apiResponse);
        return apiResponse;
    }

    public TourismRegionApiResponse getLegalDongRegions() {
        String decodedServiceKey = decodeServiceKey(serviceKey);
        TourismRegionApiResponse apiResponse = executeWithRetry(() -> restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/ldongCode2")
                        .queryParam("serviceKey", "{serviceKey}")
                        .queryParam("numOfRows", 100)
                        .queryParam("pageNo", 1)
                        .queryParam("MobileOS", mobileOs)
                        .queryParam("MobileApp", mobileApp)
                        .queryParam("_type", "json")
                        .build(decodedServiceKey))
                .retrieve().body(TourismRegionApiResponse.class), "관광 API 법정동 코드");
        validateRegionResponse(apiResponse);
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

    private void validateRegionResponse(TourismRegionApiResponse apiResponse) {
        if (apiResponse == null || apiResponse.response() == null
                || apiResponse.response().header() == null) {
            throw new TourismApiException("관광 API 법정동 코드 응답 헤더가 없습니다.");
        }
        TourismHeader header = apiResponse.response().header();
        if (!SUCCESS_CODE.equals(header.resultCode())) {
            throw new TourismApiException("관광 API 법정동 코드 오류: resultCode="
                    + header.resultCode() + ", resultMsg=" + header.resultMsg());
        }
    }

    private <T> T executeWithRetry(Supplier<T> request, String operation) {
        TourismApiException last = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                return request.get();
            } catch (RestClientResponseException exception) {
                int status = exception.getStatusCode().value();
                if ((status == 429 || status >= 500) && attempt < 3) {
                    last = new TourismApiException(operation + " 일시 오류(HTTP " + status + ")");
                    continue;
                }
                throw new TourismApiException(operation + " HTTP 오류: " + status);
            } catch (RestClientException exception) {
                last = new TourismApiException(operation + " 요청에 실패했습니다.");
                if (attempt == 3) throw last;
            }
        }
        throw last == null ? new TourismApiException(operation + " 요청에 실패했습니다.") : last;
    }
}
