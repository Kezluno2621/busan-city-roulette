package com.cityroulette.integration.tourapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonAlias;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TourismRegionDto(
        @JsonAlias("code") String lDongRegnCd,
        @JsonAlias("name") String lDongRegnNm,
        String lDongSignguCd,
        String lDongSignguNm
) { }
