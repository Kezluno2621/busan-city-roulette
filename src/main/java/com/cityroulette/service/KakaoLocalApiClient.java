package com.cityroulette.service;

import com.cityroulette.dto.location.KakaoPlaceSearchResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

public class KakaoLocalApiClient {

    private static final String BUSAN_RECT = "128.45,34.75,129.35,35.40";

    private final RestClient restClient;
    private final String restApiKey;

    public KakaoLocalApiClient(RestClient restClient, String restApiKey) {
        this.restClient = restClient;
        this.restApiKey = restApiKey;
    }

    public KakaoPlaceSearchResponse searchPlaces(String query, int size) {
        String key = requireApiKey();

        try {
            KakaoPlaceSearchResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v2/local/search/keyword.json")
                            .queryParam("query", query)
                            .queryParam("rect", BUSAN_RECT)
                            .queryParam("size", size)
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, "KakaoAK " + key)
                    .retrieve()
                    .body(KakaoPlaceSearchResponse.class);

            if (response == null || response.documents() == null) {
                throw new KakaoLocalApiException("Kakao 장소 검색 응답이 없습니다.");
            }
            return response;
        } catch (KakaoLocalApiException exception) {
            throw exception;
        } catch (RestClientResponseException exception) {
            throw new KakaoLocalApiException(
                    "Kakao 장소 검색 HTTP 오류: status=" + exception.getStatusCode().value(),
                    exception);
        } catch (RestClientException exception) {
            throw new KakaoLocalApiException("Kakao 장소 검색 호출에 실패했습니다.", exception);
        }
    }

    private String requireApiKey() {
        if (restApiKey == null || restApiKey.isBlank()) {
            throw new KakaoLocalApiException("KAKAO_REST_API_KEY 환경변수가 설정되지 않았습니다.");
        }
        return restApiKey.trim();
    }
}
