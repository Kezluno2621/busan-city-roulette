package com.cityroulette.integration.tourapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TourismRegionBody(TourismRegionItems items, int numOfRows, int pageNo, int totalCount) { }
