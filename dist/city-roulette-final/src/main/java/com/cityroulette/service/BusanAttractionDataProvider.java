package com.cityroulette.service;
import com.cityroulette.dto.importing.NormalizedProviderPage;
import org.springframework.stereotype.Component;
@Component public class BusanAttractionDataProvider implements PlaceDataProvider {
    private final BusanAttractionClient client; private final BusanAttractionPlaceMapper mapper;
    public BusanAttractionDataProvider(BusanAttractionClient client, BusanAttractionPlaceMapper mapper) { this.client=client; this.mapper=mapper; }
    public String provider(){ return "BUSAN_ATTRACTION"; }
    public NormalizedProviderPage fetch(int page,int size){ return ProviderMappingSupport.map(client.getPage(page,size),mapper); }
}
