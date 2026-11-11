package com.cityroulette.service;

import com.cityroulette.domain.*;
import com.cityroulette.dto.bus.*;
import com.cityroulette.repository.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.*;

@Service @Transactional(readOnly=true)
public class BusQueryService {
    private static final double METERS_PER_LATITUDE_DEGREE=110_000.0;
    private static final int MAX_NEARBY_RESULTS=100;
    private final BusStopRepository stops;private final BusRouteRepository routes;private final BusRouteStopRepository routeStops;private final PlaceRepository places;
    public BusQueryService(BusStopRepository stops,BusRouteRepository routes,BusRouteStopRepository routeStops,PlaceRepository places){this.stops=stops;this.routes=routes;this.routeStops=routeStops;this.places=places;}
    public BusPageResponse<BusStopResponse> stops(String name,int page,int size){Pageable pageable=PageRequest.of(page,size,Sort.by("name","id"));Page<BusStop> data=name==null||name.isBlank()?stops.findAll(pageable):stops.findByNameContainingIgnoreCase(name.trim(),pageable);return page(data.map(this::stop));}
    public BusStopResponse stop(Long id){return stop(stops.findById(id).orElseThrow(()->new BusResourceNotFoundException("정류소",id)));}
    public BusPageResponse<BusRouteResponse> routes(String number,int page,int size){Pageable pageable=PageRequest.of(page,size,Sort.by("routeNumber","id"));Page<BusRoute> data=number==null||number.isBlank()?routes.findAll(pageable):routes.findByRouteNumberContainingIgnoreCase(number.trim(),pageable);return page(data.map(this::route));}
    public BusRouteResponse route(Long id){return route(routes.findById(id).orElseThrow(()->new BusResourceNotFoundException("노선",id)));}
    public List<BusRouteStopResponse> routeStops(Long id){if(!routes.existsById(id))throw new BusResourceNotFoundException("노선",id);return routeStops.findByBusRouteIdOrderByStopSequence(id).stream().map(x->new BusRouteStopResponse(x.getStopSequence(),x.getDirection(),stop(x.getBusStop()))).toList();}
    public List<BusRouteResponse> stopRoutes(Long stopId){if(!stops.existsById(stopId))throw new BusResourceNotFoundException("정류소",stopId);return routeStops.findByBusStopIdOrderByBusRouteRouteNumberAsc(stopId).stream().map(BusRouteStop::getBusRoute).distinct().map(this::route).toList();}
    public Map<Long,List<BusRouteResponse>> routesByStopIds(Collection<Long> stopIds){
        if(stopIds==null||stopIds.isEmpty())return Map.of();
        Map<Long,LinkedHashMap<Long,BusRouteResponse>> grouped=new LinkedHashMap<>();
        for(BusRouteStop link:routeStops.findWithRoutesByBusStopIds(stopIds)){
            Long stopId=link.getBusStop().getId();BusRouteResponse response=route(link.getBusRoute());
            grouped.computeIfAbsent(stopId,key->new LinkedHashMap<>()).putIfAbsent(response.id(),response);
        }
        Map<Long,List<BusRouteResponse>> result=new LinkedHashMap<>();
        grouped.forEach((stopId,byRoute)->result.put(stopId,List.copyOf(byRoute.values())));
        return result;
    }
    public NearbyBusStopResponse nearby(Long placeId,int radius){
        Place place=places.findById(placeId).orElseThrow(()->new PlaceNotFoundException(placeId));
        if(place.getLatitude()==null||place.getLongitude()==null)throw new PlaceCoordinatesMissingException(placeId);
        double latitude=place.getLatitude(),longitude=place.getLongitude();
        double latitudeDelta=radius/METERS_PER_LATITUDE_DEGREE;
        double longitudeDelta=radius/(METERS_PER_LATITUDE_DEGREE*Math.max(Math.cos(Math.toRadians(latitude)),0.000001));
        List<NearbyBusStopResponse.NearbyStop> result=stops.findWithinBoundingBox(
                        BigDecimal.valueOf(latitude-latitudeDelta),BigDecimal.valueOf(latitude+latitudeDelta),
                        BigDecimal.valueOf(longitude-longitudeDelta),BigDecimal.valueOf(longitude+longitudeDelta)).stream()
                .map(s->new NearbyBusStopResponse.NearbyStop(stop(s),distance(latitude,longitude,s.getLatitude().doubleValue(),s.getLongitude().doubleValue())))
                .filter(x->x.distanceMeters()<=radius)
                .sorted(Comparator.comparingDouble(NearbyBusStopResponse.NearbyStop::distanceMeters))
                .limit(MAX_NEARBY_RESULTS).toList();
        return new NearbyBusStopResponse(placeId,radius,"HAVERSINE_STRAIGHT_LINE_NOT_WALKING_DISTANCE",result);
    }
    public boolean hasNearbyStop(double latitude,double longitude,int radius){
        double latitudeDelta=radius/METERS_PER_LATITUDE_DEGREE;
        double longitudeDelta=radius/(METERS_PER_LATITUDE_DEGREE*Math.max(Math.cos(Math.toRadians(latitude)),0.000001));
        return stops.findWithinBoundingBox(
                        BigDecimal.valueOf(latitude-latitudeDelta),BigDecimal.valueOf(latitude+latitudeDelta),
                        BigDecimal.valueOf(longitude-longitudeDelta),BigDecimal.valueOf(longitude+longitudeDelta)).stream()
                .anyMatch(stop->distance(latitude,longitude,stop.getLatitude().doubleValue(),stop.getLongitude().doubleValue())<=radius);
    }
    double distance(double lat1,double lon1,double lat2,double lon2){double p1=Math.toRadians(lat1),p2=Math.toRadians(lat2),dLat=p2-p1,dLon=Math.toRadians(lon2-lon1);double a=Math.sin(dLat/2)*Math.sin(dLat/2)+Math.cos(p1)*Math.cos(p2)*Math.sin(dLon/2)*Math.sin(dLon/2);return 6371000*2*Math.atan2(Math.sqrt(a),Math.sqrt(1-a));}
    private BusStopResponse stop(BusStop s){return new BusStopResponse(s.getId(),s.getExternalStopId(),s.getArsNumber(),s.getName(),s.getLatitude(),s.getLongitude(),s.getStopType());}
    private BusRouteResponse route(BusRoute r){return new BusRouteResponse(r.getId(),r.getExternalRouteId(),r.getRouteNumber(),r.getRouteType(),r.getStartStopName(),r.getEndStopName());}
    private <T> BusPageResponse<T> page(Page<T> p){return new BusPageResponse<>(p.getContent(),p.getNumber(),p.getSize(),p.getTotalElements(),p.getTotalPages());}
}
