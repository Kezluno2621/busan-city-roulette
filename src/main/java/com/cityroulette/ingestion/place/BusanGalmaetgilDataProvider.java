package com.cityroulette.ingestion.place;

import com.cityroulette.ingestion.place.mapper.BusanGalmaetgilPlaceMapper;
import com.cityroulette.integration.busan.BusanGalmaetgilClient;
import com.cityroulette.ingestion.place.dto.NormalizedProviderPage;
import org.springframework.stereotype.Component;

/** 갈맷길 API의 페이지를 읽고 공급자 전용 Mapper를 거쳐 공통 수집 모델로 전달합니다. */
@Component
public class BusanGalmaetgilDataProvider implements PlaceDataProvider {
    private final BusanGalmaetgilClient client;
    private final BusanGalmaetgilPlaceMapper mapper;

    public BusanGalmaetgilDataProvider(BusanGalmaetgilClient client, BusanGalmaetgilPlaceMapper mapper) {
        this.client = client;
        this.mapper = mapper;
    }

    public String provider() {
        return "BUSAN_GALMAETGIL";
    }

    public NormalizedProviderPage fetch(int page, int size) {
        return ProviderMappingSupport.map(client.getPage(page, size), mapper);
    }
}
