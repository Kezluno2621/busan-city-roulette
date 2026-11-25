package com.cityroulette.dto.importing;

import java.util.List;

public record NormalizedProviderPage(List<NormalizedPlace> items, int pageNo, int totalCount, int mappingFailures) { }
