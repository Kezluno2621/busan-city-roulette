package com.cityroulette.ingestion.place.provider;

import com.cityroulette.ingestion.place.mapper.BusanRestaurantPlaceMapper;
import com.cityroulette.integration.busan.BusanRestaurantClient;
import com.cityroulette.ingestion.place.dto.NormalizedProviderPage;
import org.springframework.stereotype.Component;

/** 부산 맛집 API의 페이지를 읽고 공급자 전용 Mapper를 거쳐 공통 수집 모델로 전달합니다. */
@Component
public class BusanRestaurantDataProvider implements PlaceDataProvider {
    private final BusanRestaurantClient client;
    private final BusanRestaurantPlaceMapper mapper;

    public BusanRestaurantDataProvider(BusanRestaurantClient client, BusanRestaurantPlaceMapper mapper) {
        this.client = client;
        this.mapper = mapper;
    }

    public String provider() {
        return "BUSAN_RESTAURANT";
    }

    public NormalizedProviderPage fetch(int page, int size) {
        return ProviderMappingSupport.map(client.getPage(page, size), mapper);
    }
}
