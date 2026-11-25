package com.cityroulette.service;
import com.cityroulette.dto.importing.NormalizedProviderPage;
import org.springframework.stereotype.Component;
@Component public class BusanRestaurantDataProvider implements PlaceDataProvider {
    private final BusanRestaurantClient client; private final BusanRestaurantPlaceMapper mapper;
    public BusanRestaurantDataProvider(BusanRestaurantClient client, BusanRestaurantPlaceMapper mapper) { this.client=client; this.mapper=mapper; }
    public String provider(){ return "BUSAN_RESTAURANT"; }
    public NormalizedProviderPage fetch(int page,int size){ return ProviderMappingSupport.map(client.getPage(page,size),mapper); }
}
