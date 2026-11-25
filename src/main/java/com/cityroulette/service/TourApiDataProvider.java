package com.cityroulette.service;

import com.cityroulette.dto.importing.NormalizedProviderPage;
import com.cityroulette.dto.importing.ProviderPage;
import org.springframework.stereotype.Component;

/**
 * 수집 서비스에서 공급자별 API와 응답 형식의 차이를 다룰 수 있도록 연결합니다.
 */
@Component
public class TourApiDataProvider implements PlaceDataProvider {
    private final TourismService client; private final TourApiPlaceMapper mapper;
    public TourApiDataProvider(TourismService client, TourApiPlaceMapper mapper) { this.client = client; this.mapper = mapper; }
    public String provider() { return "TOUR_API"; }
    public NormalizedProviderPage fetch(int pageNo, int pageSize) {
        if (pageNo == 1) client.verifyBusanLegalDongCode();
        var page = client.getBusanPlacesPage(pageNo, pageSize);
        var busanItems = page.items().stream().filter(item -> "26".equals(item.lDongRegnCd())).toList();
        return ProviderMappingSupport.map(new ProviderPage<>(busanItems, pageNo, pageSize, page.totalCount()), mapper);
    }
}
