package com.cityroulette.domain.transport.dto;
import java.util.List;
public record NearbyBusStopResponse(Long placeId,int radiusMeters,String distanceType,List<NearbyStop> stops) {
    public record NearbyStop(BusStopResponse stop,double distanceMeters) { }
}
