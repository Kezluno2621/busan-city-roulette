package com.cityroulette.integration.odsay.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OdsayPathInfoDto(
        Integer totalTime,
        Integer totalWalk,
        Integer payment,
        Integer busTransitCount,
        Integer subwayTransitCount,
        String firstStartStation,
        String lastEndStation) {
}
