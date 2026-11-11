package com.cityroulette.service;

import com.cityroulette.dto.importing.NormalizedPlace;

public interface ProviderPlaceMapper<T> {
    NormalizedPlace map(T source);
}
