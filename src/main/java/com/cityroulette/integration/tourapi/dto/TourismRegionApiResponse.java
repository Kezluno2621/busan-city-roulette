package com.cityroulette.integration.tourapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TourismRegionApiResponse(TourismRegionResponseEnvelope response) { }
