package com.cityroulette.dto.tourism;

import java.util.List;

public record TourismPage(List<TourismPlaceDto> items, int pageNo, int pageSize, int totalCount) { }
