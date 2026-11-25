package com.cityroulette.service;

import com.cityroulette.dto.importing.NormalizedPlace;
import com.cityroulette.dto.importing.NormalizedProviderPage;
import com.cityroulette.dto.importing.ProviderPage;

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
