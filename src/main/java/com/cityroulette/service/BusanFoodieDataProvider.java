package com.cityroulette.service;

import com.cityroulette.dto.importing.NormalizedProviderPage;
import org.springframework.stereotype.Component;

/** 부산푸디투어 API의 페이지를 읽고 공급자 전용 Mapper를 거쳐 공통 수집 모델로 전달합니다. */
@Component
public class BusanFoodieDataProvider implements PlaceDataProvider {
    private final BusanFoodieClient client;
    private final BusanFoodiePlaceMapper mapper;

    public BusanFoodieDataProvider(BusanFoodieClient client, BusanFoodiePlaceMapper mapper) {
        this.client = client;
        this.mapper = mapper;
    }

    public String provider() {
        return "BUSAN_FOODIE";
    }

    public NormalizedProviderPage fetch(int page, int size) {
        return ProviderMappingSupport.map(client.getPage(page, size), mapper);
    }
}
