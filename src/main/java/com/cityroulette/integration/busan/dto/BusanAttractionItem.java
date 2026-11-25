package com.cityroulette.integration.busan.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 외부 응답에서 읽은 원천 값을 담아 이후 검증과 변환 단계로 전달합니다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BusanAttractionItem(
        @JsonProperty("UC_SEQ") String externalId,
        @JsonProperty("MAIN_TITLE") String mainTitle,
        @JsonProperty("TITLE") String title,
        @JsonProperty("ADDR1") String address,
        @JsonProperty("GUGUN_NM") String district,
        @JsonProperty("LAT") String latitude,
        @JsonProperty("LNG") String longitude,
        @JsonProperty("ITEMCNTNTS") String description,
        @JsonProperty("MAIN_IMG_NORMAL") String imageUrl,
        @JsonProperty("CNTCT_TEL") String telephone) { }
