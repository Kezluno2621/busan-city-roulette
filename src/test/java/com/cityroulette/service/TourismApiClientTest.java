package com.cityroulette.service;

import com.cityroulette.dto.tourism.TourismApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class TourismApiClientTest {

    private static final String BASE_URL = "https://tourism.test/KorService2";

    private MockRestServiceServer server;
    private TourismApiClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        client = new TourismApiClient(
                builder.build(), "encoded%2Bkey%2Fvalue%3D", "ETC", "CityRoulette");
    }

    @Test
    void requestsBusanTouristPlacesAndDeserializesResponse() throws Exception {
        String responseBody = new ClassPathResource("tourism/area-based-list-sample.json")
                .getContentAsString(StandardCharsets.UTF_8);

        server.expect(request -> {
                    assertThat(request.getURI().getPath())
                            .isEqualTo("/KorService2/areaBasedList2");
                    assertThat(request.getURI().getRawQuery())
                            .contains("serviceKey=encoded%2Bkey%2Fvalue%3D")
                            .doesNotContain("%252B")
                            .contains("numOfRows=25", "pageNo=2", "MobileOS=ETC",
                                    "MobileApp=CityRoulette", "_type=json", "arrange=C",
                                    "contentTypeId=12", "lDongRegnCd=26");
                })
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        TourismApiResponse result = client.getBusanTouristPlaces(2, 25);

        assertThat(result.response().header().resultCode()).isEqualTo("0000");
        assertThat(result.response().body().items().item()).hasSize(2);
        server.verify();
    }

    @Test
    void rejectsApiErrorResponse() {
        server.expect(request -> assertThat(request.getURI().getPath())
                        .isEqualTo("/KorService2/areaBasedList2"))
                .andRespond(withSuccess("""
                        {"response":{"header":{"resultCode":"22","resultMsg":"LIMITED NUMBER OF SERVICE REQUESTS EXCEEDS ERROR"}}}
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.getBusanTouristPlaces(1, 10))
                .isInstanceOf(TourismApiException.class)
                .hasMessageContaining("resultCode=22")
                .hasMessageContaining("resultMsg=LIMITED NUMBER OF SERVICE REQUESTS EXCEEDS ERROR")
                .hasMessageNotContaining("encoded%2Bkey");
        server.verify();
    }

    @Test
    void rejectsMissingApiKeyBeforeSendingRequest() {
        TourismApiClient clientWithoutKey = new TourismApiClient(
                RestClient.create(BASE_URL), " ", "ETC", "CityRoulette");

        assertThatThrownBy(() -> clientWithoutKey.getBusanTouristPlaces(1, 10))
                .isInstanceOf(TourismApiException.class)
                .hasMessage("TOUR_API_KEY 환경변수가 설정되지 않았습니다.");
    }
}
