package com.cityroulette.dto.importing;

import java.util.List;

public record ProviderPage<T>(List<T> items, int pageNo, int pageSize, int totalCount) { }
