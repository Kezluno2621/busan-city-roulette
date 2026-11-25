package com.cityroulette.integration.tourapi.dto;

import java.util.List;

public record TourismPage(List<TourismPlaceDto> items, int pageNo, int pageSize, int totalCount) { }
