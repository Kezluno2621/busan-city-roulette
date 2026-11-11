package com.cityroulette.service;

import com.cityroulette.dto.importing.NormalizedProviderPage;

public interface PlaceDataProvider {
    String provider();
    NormalizedProviderPage fetch(int pageNo, int pageSize);
}
