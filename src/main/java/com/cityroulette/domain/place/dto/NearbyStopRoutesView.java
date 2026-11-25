package com.cityroulette.domain.place.dto;

import com.cityroulette.domain.transport.dto.BusRouteResponse;
import com.cityroulette.domain.transport.dto.BusStopResponse;
import java.util.List;
public record NearbyStopRoutesView(BusStopResponse stop,double distanceMeters,List<BusRouteResponse> routes) { }
