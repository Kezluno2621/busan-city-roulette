package com.cityroulette.dto.tourism;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TourismResponse(TourismHeader header, TourismBody body) {
}
