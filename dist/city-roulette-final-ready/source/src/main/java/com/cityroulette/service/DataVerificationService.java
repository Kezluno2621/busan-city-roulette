package com.cityroulette.service;

import com.cityroulette.domain.Place;
import com.cityroulette.dto.DataVerificationSnapshot;
import com.cityroulette.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly=true)
public class DataVerificationService {
    private final PlaceRepository places;
    private final PlaceSourceRepository sources;
    private final BusStopRepository stops;
    private final BusRouteRepository routes;
    private final BusRouteStopRepository routeStops;
    private final BusQueryService busQueries;

    public DataVerificationService(PlaceRepository places,PlaceSourceRepository sources,BusStopRepository stops,
            BusRouteRepository routes,BusRouteStopRepository routeStops,BusQueryService busQueries){
        this.places=places;this.sources=sources;this.stops=stops;this.routes=routes;
        this.routeStops=routeStops;this.busQueries=busQueries;
    }

    public DataVerificationSnapshot snapshot(){
        List<Place> locatedPlaces=places.findByLatitudeIsNotNullAndLongitudeIsNotNull();
        return new DataVerificationSnapshot(
                places.count(),sources.count(),locatedPlaces.size(),grouped(places.countByCategory()),grouped(places.countByDistrict()),
                stops.count(),stops.countWithValidCoordinates(),routes.count(),routeStops.count(),
                routeStops.countRoutesWithStops(),routeStops.countRoutesWithoutStops(),
                matchingRate(locatedPlaces,300),matchingRate(locatedPlaces,500),matchingRate(locatedPlaces,1000));
    }

    private double matchingRate(List<Place> locatedPlaces,int radius){
        if(locatedPlaces.isEmpty())return 0.0;
        long matched=locatedPlaces.stream().filter(place->busQueries.hasNearbyStop(place.getLatitude(),place.getLongitude(),radius)).count();
        return Math.round(matched*10_000.0/locatedPlaces.size())/100.0;
    }

    private Map<String,Long> grouped(List<Object[]> rows){
        Map<String,Long> result=new LinkedHashMap<>();
        for(Object[] row:rows)result.put(row[0]==null?"(UNKNOWN)":row[0].toString(),(Long)row[1]);
        return result;
    }
}
