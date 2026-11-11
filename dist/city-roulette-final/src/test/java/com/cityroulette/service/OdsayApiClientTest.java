package com.cityroulette.service;

import com.cityroulette.dto.transit.OdsayResponse;
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

class OdsayApiClientTest {

    private static final String BASE_URL = "https://odsay.test/v1/api";

    private MockRestServiceServer server;
    private OdsayApiClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        client = new OdsayApiClient(builder.build(), "key+value/test=");
    }

    @Test
    void requestsOneRouteSearchAndDeserializesResponse() throws Exception {
        String responseBody = new ClassPathResource(
                "odsay/search-public-transit-sample.json")
                .getContentAsString(StandardCharsets.UTF_8);

        server.expect(request -> {
                    assertThat(request.getURI().getPath())
                            .isEqualTo("/v1/api/searchPubTransPathT");
                    assertThat(request.getURI().getRawQuery())
                            .contains("SX=129.0105969965")
                            .contains("SY=35.0974606978")
                            .contains("EX=129.123566")
                            .contains("EY=35.115449")
                            .contains("OPT=0", "SearchType=0", "SearchPathType=0", "lang=0")
                            .contains("apiKey=key%2Bvalue%2Ftest%3D")
                            .doesNotContain("%252B");
                })
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        OdsayResponse response = client.searchPublicTransit(
                129.0105969965, 35.0974606978, 129.123566, 35.115449);

        assertThat(response.result().path()).hasSize(3);
        server.verify();
    }

    @Test
    void rejectsObjectErrorWithoutExposingApiKey() {
        server.expect(request -> assertThat(request.getURI().getPath())
                        .isEqualTo("/v1/api/searchPubTransPathT"))
                .andRespond(withSuccess("""
                        {"error":{"code":"-98","msg":"API limit exceeded"}}
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.searchPublicTransit(129, 35, 129.1, 35.1))
                .isInstanceOf(OdsayApiException.class)
                .hasMessageContaining("code=-98")
                .hasMessageContaining("message=API limit exceeded")
                .hasMessageNotContaining("key+value");
        server.verify();
    }

    @Test
    void rejectsArrayErrorWithoutExposingApiKey() {
        server.expect(request -> assertThat(request.getURI().getPath())
                        .isEqualTo("/v1/api/searchPubTransPathT"))
                .andRespond(withSuccess("""
                        {"error":[{"code":"500","message":"ApiKey authentication failed"}]}
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.searchPublicTransit(129, 35, 129.1, 35.1))
                .isInstanceOf(OdsayApiException.class)
                .hasMessageContaining("code=500")
                .hasMessageContaining("ApiKey authentication failed")
                .hasMessageNotContaining("key+value");
        server.verify();
    }

    @Test
    void identifiesNoRouteErrorCode() {
        server.expect(request -> assertThat(request.getURI().getPath())
                        .isEqualTo("/v1/api/searchPubTransPathT"))
                .andRespond(withSuccess("""
                        {"error":{"code":"400","msg":"검색 데이터 없음"}}
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.searchPublicTransit(129, 35, 129.1, 35.1))
                .isInstanceOfSatisfying(OdsayApiException.class,
                        exception -> assertThat(exception.isRouteNotFound()).isTrue());
        server.verify();
    }

    @Test
    void rejectsMissingApiKeyBeforeSendingRequest() {
        OdsayApiClient clientWithoutKey = new OdsayApiClient(
                RestClient.create(BASE_URL), " ");

        assertThatThrownBy(() -> clientWithoutKey.searchPublicTransit(129, 35, 129.1, 35.1))
                .isInstanceOf(OdsayApiException.class)
                .hasMessage("ODSAY_API_KEY 환경변수가 설정되지 않았습니다.");
    }
}
