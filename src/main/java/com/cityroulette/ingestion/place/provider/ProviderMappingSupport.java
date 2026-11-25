package com.cityroulette.ingestion.place.provider;

import com.cityroulette.ingestion.place.mapper.ProviderPlaceMapper;
import com.cityroulette.ingestion.place.dto.NormalizedPlace;
import com.cityroulette.ingestion.place.dto.NormalizedProviderPage;
import com.cityroulette.integration.busan.dto.ProviderPage;

import java.util.ArrayList;

final class ProviderMappingSupport {
    private ProviderMappingSupport() { }
    static <T> NormalizedProviderPage map(ProviderPage<T> page, ProviderPlaceMapper<T> mapper) {
        var mapped = new ArrayList<NormalizedPlace>();
        int failures = 0;
        for (T item : page.items()) {
            try { mapped.add(mapper.map(item)); }
            catch (RuntimeException exception) { failures++; }
        }
        return new NormalizedProviderPage(mapped, page.pageNo(), page.totalCount(), failures);
    }
}
