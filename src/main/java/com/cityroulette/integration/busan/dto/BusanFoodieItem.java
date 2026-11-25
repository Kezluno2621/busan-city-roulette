package com.cityroulette.integration.busan.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 외부 응답에서 읽은 원천 값을 담아 이후 검증과 변환 단계로 전달합니다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BusanFoodieItem(
        @JsonProperty("UC_SEQ") String externalId,
        @JsonProperty("MAIN_TITLE") String mainTitle,
        @JsonProperty("PLACE") String place,
        @JsonProperty("TITLE") String title,
        @JsonProperty("SUBTITLE") String subtitle,
        @JsonProperty("ITEMCNTNTS") String description,
        @JsonProperty("MAIN_IMG_NORMAL") String imageUrl) { }
