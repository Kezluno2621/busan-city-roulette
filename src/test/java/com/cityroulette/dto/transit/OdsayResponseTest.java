package com.cityroulette.dto.transit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

class OdsayResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void deserializesRequiredRouteFields() throws Exception {
        try (InputStream json = getClass().getResourceAsStream(
                "/odsay/search-public-transit-sample.json")) {
            assertThat(json).isNotNull();

            OdsayResponse response = objectMapper.readValue(json, OdsayResponse.class);
            OdsayPathDto firstPath = response.result().path().get(0);

            assertThat(response.result().path()).hasSize(3);
            assertThat(firstPath.pathType()).isEqualTo(2);
            assertThat(firstPath.info().totalTime()).isEqualTo(82);
            assertThat(firstPath.info().totalWalk()).isEqualTo(1182);
            assertThat(firstPath.info().payment()).isEqualTo(1550);
            assertThat(firstPath.info().busTransitCount()).isEqualTo(1);
            assertThat(firstPath.info().subwayTransitCount()).isZero();
            assertThat(firstPath.info().firstStartStation()).isEqualTo("아미초등학교");
            assertThat(firstPath.info().lastEndStation()).isEqualTo("용호동경동아파트");
        }
    }
}
