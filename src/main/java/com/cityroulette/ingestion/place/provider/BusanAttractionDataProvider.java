package com.cityroulette.ingestion.place.provider;

import com.cityroulette.ingestion.place.mapper.BusanAttractionPlaceMapper;
import com.cityroulette.integration.busan.BusanAttractionClient;
import com.cityroulette.ingestion.place.dto.NormalizedProviderPage;
import org.springframework.stereotype.Component;

/** 부산 명소 API의 페이지를 읽고 공급자 전용 Mapper를 거쳐 공통 수집 모델로 전달합니다. */
@Component
public class BusanAttractionDataProvider implements PlaceDataProvider {
    private final BusanAttractionClient client;
    private final BusanAttractionPlaceMapper mapper;

    public BusanAttractionDataProvider(BusanAttractionClient client, BusanAttractionPlaceMapper mapper) {
        this.client = client;
        this.mapper = mapper;
    }

    public String provider() {
        return "BUSAN_ATTRACTION";
    }

    public NormalizedProviderPage fetch(int page, int size) {
        return ProviderMappingSupport.map(client.getPage(page, size), mapper);
    }
}
