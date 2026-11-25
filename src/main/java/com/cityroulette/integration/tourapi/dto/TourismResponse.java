package com.cityroulette.integration.tourapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TourismResponse(TourismHeader header, TourismBody body) {
}
