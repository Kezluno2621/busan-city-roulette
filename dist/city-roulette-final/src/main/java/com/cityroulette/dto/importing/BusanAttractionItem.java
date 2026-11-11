package com.cityroulette.dto.importing;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

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
