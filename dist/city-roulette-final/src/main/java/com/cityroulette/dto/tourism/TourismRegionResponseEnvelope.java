package com.cityroulette.dto.tourism;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TourismRegionResponseEnvelope(TourismHeader header, TourismRegionBody body) { }
