package com.cityroulette.service;

import com.cityroulette.dto.external.ExternalApiProbeResult;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import org.springframework.http.HttpStatus;

class BusanJsonApiClientTest {
    @Test
    void parsesSuccessfulWrappedResponse() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://busan.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(request -> {
            assertThat(request.getURI().getPath()).isEqualTo("/sample");
            assertThat(request.getURI().getRawQuery()).contains("pageNo=1", "numOfRows=1", "resultType=json");
        }).andRespond(withSuccess("""
                {"getFoodKr":{"header":{"code":"00","message":"OK"},
                "body":{"totalCount":37,"items":{"item":[{"UC_SEQ":"1"}]}}}}
                """, MediaType.APPLICATION_JSON));
        BusanJsonApiClient client = new TestClient(builder.build(), "encoded%2Bkey");

        ExternalApiProbeResult result = client.probe();

        assertThat(result.status()).isEqualTo(ExternalApiProbeResult.Status.CONNECTED);
        assertThat(result.sampleCount()).isEqualTo(1);
        assertThat(result.totalCount()).isEqualTo(37);
        assertThat(result.httpStatus()).isEqualTo(200);
        assertThat(result.providerCode()).isEqualTo("00");
        assertThat(result.responseFields()).contains("UC_SEQ");
        server.verify();
    }

    @Test
    void returnsNotConfiguredWithoutRequest() {
        BusanJsonApiClient client = new TestClient(RestClient.create("https://busan.test"), " ");
        assertThat(client.probe().status()).isEqualTo(ExternalApiProbeResult.Status.NOT_CONFIGURED);
    }

    @Test
    void parsesBusanResponseWithItemsDirectlyUnderOperationWrapper() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://busan.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(request -> { }).andRespond(withSuccess("""
                {"getAttractionKr":{"header":{"code":"00","message":"OK"},
                "item":[{"UC_SEQ":"255","MAIN_TITLE":"흰여울문화마을","LAT":"35.0"}],
                "numOfRows":1,"pageNo":1,"totalCount":107}}
                """, MediaType.APPLICATION_JSON));

        ExternalApiProbeResult result = new TestClient(builder.build(), "key").probe();

        assertThat(result.sampleCount()).isEqualTo(1);
        assertThat(result.totalCount()).isEqualTo(107);
        assertThat(result.responseFields()).containsExactlyInAnyOrder("UC_SEQ", "MAIN_TITLE", "LAT");
    }

    @Test
    void reportsBodyErrorWithoutExposingKey() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://busan.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(request -> { }).andRespond(withSuccess("""
                {"response":{"header":{"resultCode":"20","resultMsg":"PERMISSION_DENIED"}}}
                """, MediaType.APPLICATION_JSON));
        BusanJsonApiClient client = new TestClient(builder.build(), "secret-key");

        ExternalApiProbeResult result = client.probe();

        assertThat(result.status()).isEqualTo(ExternalApiProbeResult.Status.AUTH_FAILED);
        assertThat(result.message()).contains("code=20").doesNotContain("secret-key");
    }

    @Test
    void distinguishesHttpFailureFromAuthenticationAndParsing() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://busan.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(request -> { }).andRespond(withStatus(HttpStatus.BAD_GATEWAY));

        ExternalApiProbeResult result = new TestClient(builder.build(), "secret-key").probe();

        assertThat(result.status()).isEqualTo(ExternalApiProbeResult.Status.HTTP_ERROR);
        assertThat(result.httpStatus()).isEqualTo(502);
        assertThat(result.message()).doesNotContain("secret-key");
    }

    private static class TestClient extends BusanJsonApiClient {
        TestClient(RestClient restClient, String key) {
            super(restClient, "TEST", "/sample", key);
        }
    }
}
