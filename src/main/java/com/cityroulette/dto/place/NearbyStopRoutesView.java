package com.cityroulette.dto.place;
import com.cityroulette.dto.bus.*;
import java.util.List;
public record NearbyStopRoutesView(BusStopResponse stop,double distanceMeters,List<BusRouteResponse> routes) { }
