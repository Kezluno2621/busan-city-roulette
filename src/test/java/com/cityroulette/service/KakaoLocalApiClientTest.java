package com.cityroulette.service;

import com.cityroulette.dto.location.KakaoPlaceSearchResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class KakaoLocalApiClientTest {

    private static final String BASE_URL = "https://dapi.kakao.test";

    private MockRestServiceServer server;
    private KakaoLocalApiClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        client = new KakaoLocalApiClient(builder.build(), "secret-key");
    }

    @Test
    void searchesBusanPlacesWithAuthorizationHeader() {
        server.expect(request -> assertThat(request.getURI().getPath())
                        .isEqualTo("/v2/local/search/keyword.json"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "KakaoAK secret-key"))
                .andExpect(queryParam("query", "%EB%B6%80%EC%82%B0%EC%97%AD"))
                .andExpect(queryParam("rect", "128.45,34.75,129.35,35.40"))
                .andExpect(queryParam("size", "10"))
                .andRespond(withSuccess("""
                        {
                          "documents": [{
                            "id": "101",
                            "place_name": "부산역",
                            "category_name": "교통,수송 > 기차역",
                            "address_name": "부산 동구 초량동",
                            "road_address_name": "부산 동구 중앙대로 206",
                            "x": "129.0414",
                            "y": "35.1151"
                          }]
                        }
                        """, MediaType.APPLICATION_JSON));

        KakaoPlaceSearchResponse response = client.searchPlaces("부산역", 10);

        assertThat(response.documents()).hasSize(1);
        assertThat(response.documents().get(0).placeName()).isEqualTo("부산역");
        server.verify();
    }

    @Test
    void rejectsMissingApiKeyBeforeSendingRequest() {
        KakaoLocalApiClient clientWithoutKey = new KakaoLocalApiClient(
                RestClient.create(BASE_URL), " ");

        assertThatThrownBy(() -> clientWithoutKey.searchPlaces("부산역", 10))
                .isInstanceOf(KakaoLocalApiException.class)
                .hasMessage("KAKAO_REST_API_KEY 환경변수가 설정되지 않았습니다.");
    }
}
