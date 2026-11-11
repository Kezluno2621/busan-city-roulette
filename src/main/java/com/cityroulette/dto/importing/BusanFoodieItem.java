package com.cityroulette.dto.importing;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BusanFoodieItem(
        @JsonProperty("UC_SEQ") String externalId,
        @JsonProperty("MAIN_TITLE") String mainTitle,
        @JsonProperty("PLACE") String place,
        @JsonProperty("TITLE") String title,
        @JsonProperty("SUBTITLE") String subtitle,
        @JsonProperty("ITEMCNTNTS") String description,
        @JsonProperty("MAIN_IMG_NORMAL") String imageUrl) { }
