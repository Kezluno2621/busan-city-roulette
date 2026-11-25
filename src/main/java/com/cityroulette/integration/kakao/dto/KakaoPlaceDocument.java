package com.cityroulette.integration.kakao.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 외부 응답에서 읽은 원천 값을 담아 이후 검증과 변환 단계로 전달합니다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record KakaoPlaceDocument(
        String id,
        @JsonProperty("place_name") String placeName,
        @JsonProperty("category_name") String categoryName,
        @JsonProperty("address_name") String addressName,
        @JsonProperty("road_address_name") String roadAddressName,
        String x,
        String y) {
}
