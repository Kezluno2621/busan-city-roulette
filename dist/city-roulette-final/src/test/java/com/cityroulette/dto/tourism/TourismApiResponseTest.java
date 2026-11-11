package com.cityroulette.dto.tourism;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

class TourismApiResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void deserializesAreaBasedListResponse() throws Exception {
        try (InputStream json = getClass().getResourceAsStream(
                "/tourism/area-based-list-sample.json")) {
            assertThat(json).isNotNull();

            TourismApiResponse result = objectMapper.readValue(json, TourismApiResponse.class);
            TourismPlaceDto firstItem = result.response().body().items().item().get(0);

            assertThat(result.response().header().resultCode()).isEqualTo("0000");
            assertThat(result.response().header().resultMsg()).isEqualTo("OK");
            assertThat(result.response().body().totalCount()).isEqualTo(351);
            assertThat(result.response().body().items().item()).hasSize(2);
            assertThat(firstItem.contentid()).isEqualTo("3017435");
            assertThat(firstItem.title()).isEqualTo("중리노을전망대");
            assertThat(firstItem.mapx()).isEqualTo("129.0643000000");
            assertThat(firstItem.mapy()).isEqualTo("35.0692000000");
            assertThat(firstItem.lDongRegnCd()).isEqualTo("26");
        }
    }
}
