package com.cityroulette.service;
import com.cityroulette.dto.importing.NormalizedProviderPage;
import org.springframework.stereotype.Component;
@Component public class BusanFoodieDataProvider implements PlaceDataProvider {
    private final BusanFoodieClient client; private final BusanFoodiePlaceMapper mapper;
    public BusanFoodieDataProvider(BusanFoodieClient client, BusanFoodiePlaceMapper mapper) { this.client=client; this.mapper=mapper; }
    public String provider(){ return "BUSAN_FOODIE"; }
    public NormalizedProviderPage fetch(int page,int size){ return ProviderMappingSupport.map(client.getPage(page,size),mapper); }
}
