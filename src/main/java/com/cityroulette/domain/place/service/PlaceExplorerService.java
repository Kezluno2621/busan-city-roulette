package com.cityroulette.domain.place.service;

import com.cityroulette.domain.place.dto.NearbyStopRoutesView;
import com.cityroulette.domain.place.dto.PlaceDetailView;
import com.cityroulette.domain.place.dto.PlacePageResponse;
import com.cityroulette.domain.place.dto.PlaceSourceView;
import com.cityroulette.domain.transport.service.BusQueryService;
import com.cityroulette.domain.place.repository.PlaceSourceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class PlaceExplorerService {
    private final PlaceService places; private final PlaceSourceRepository sources; private final BusQueryService buses;
    public PlaceExplorerService(PlaceService places,PlaceSourceRepository sources,BusQueryService buses){this.places=places;this.sources=sources;this.buses=buses;}
    public PlacePageResponse search(String category,String district,String keyword,int page,int size){return places.searchPlaces(category,district,keyword,page,size);}
    public List<String> categories(){return places.getCategories();} public List<String> districts(){return places.getDistricts();}
    @Transactional(readOnly=true)
    public PlaceDetailView detail(Long id,int radius){
        var place=places.getPlace(id);var sourceViews=sources.findByPlaceIdOrderByProviderAsc(id).stream().map(PlaceSourceView::from).toList();
        LocalDateTime synced=sourceViews.stream().map(PlaceSourceView::lastSyncedAt).filter(Objects::nonNull).max(LocalDateTime::compareTo).orElse(place.updatedAt());
        boolean hasCoordinates=place.latitude()!=null&&place.longitude()!=null;List<NearbyStopRoutesView> nearby=List.of();
        if(hasCoordinates){
            var nearbyStops=buses.nearby(id,radius).stops().stream().limit(12).toList();
            var routesByStop=buses.routesByStopIds(nearbyStops.stream().map(x->x.stop().id()).toList());
            nearby=nearbyStops.stream().map(x->new NearbyStopRoutesView(x.stop(),x.distanceMeters(),routesByStop.getOrDefault(x.stop().id(),List.of()))).toList();
        }
        return new PlaceDetailView(place,sourceViews,synced,nearby,hasCoordinates);
    }
}
