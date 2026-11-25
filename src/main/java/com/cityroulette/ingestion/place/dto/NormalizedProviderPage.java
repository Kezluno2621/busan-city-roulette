package com.cityroulette.ingestion.place.dto;

import java.util.List;

public record NormalizedProviderPage(List<NormalizedPlace> items, int pageNo, int totalCount, int mappingFailures) { }
