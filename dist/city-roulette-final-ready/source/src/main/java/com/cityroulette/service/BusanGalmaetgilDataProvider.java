package com.cityroulette.service;
import com.cityroulette.dto.importing.NormalizedProviderPage;
import org.springframework.stereotype.Component;
@Component public class BusanGalmaetgilDataProvider implements PlaceDataProvider {
    private final BusanGalmaetgilClient client; private final BusanGalmaetgilPlaceMapper mapper;
    public BusanGalmaetgilDataProvider(BusanGalmaetgilClient client, BusanGalmaetgilPlaceMapper mapper) { this.client=client; this.mapper=mapper; }
    public String provider(){ return "BUSAN_GALMAETGIL"; }
    public NormalizedProviderPage fetch(int page,int size){ return ProviderMappingSupport.map(client.getPage(page,size),mapper); }
}
