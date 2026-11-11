package com.cityroulette.dto.tourism;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TourismItems(List<TourismPlaceDto> item) {
}
