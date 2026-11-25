package com.cityroulette.dto.tourism;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TourismRegionBody(TourismRegionItems items, int numOfRows, int pageNo, int totalCount) { }
