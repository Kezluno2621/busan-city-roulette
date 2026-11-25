package com.cityroulette.domain.transport.service;

import com.cityroulette.domain.transport.dto.TransitGraphStop;
import com.cityroulette.domain.transport.dto.TransitRouteGraph;
import com.cityroulette.domain.transport.repository.BusRouteStopRepository;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransitGraphService {
    private final BusRouteStopRepository routeStops;
    public TransitGraphService(BusRouteStopRepository routeStops){this.routeStops=routeStops;}

    @Transactional(readOnly=true)
    public TransitRouteGraph route(Long routeId){
        var links=routeStops.findGraphByRouteId(routeId);
        if(links.isEmpty())throw new BusResourceNotFoundException("버스 노선",routeId);
        return graph(links);
    }

    @Transactional(readOnly=true)
    public Map<Long,TransitRouteGraph> routes(Collection<Long> routeIds){
        if(routeIds==null||routeIds.isEmpty())return Map.of();
        Map<Long,List<com.cityroulette.domain.transport.entity.BusRouteStop>> grouped=new LinkedHashMap<>();
        for(var link:routeStops.findGraphsByRouteIds(routeIds))
            grouped.computeIfAbsent(link.getBusRoute().getId(),ignored->new ArrayList<>()).add(link);
        Map<Long,TransitRouteGraph> result=new LinkedHashMap<>();
        grouped.forEach((id,links)->result.put(id,graph(links)));
        return Collections.unmodifiableMap(result);
    }

    private TransitRouteGraph graph(List<com.cityroulette.domain.transport.entity.BusRouteStop> links){
        var route=links.get(0).getBusRoute();
        List<TransitGraphStop> stops=links.stream().map(link->{
            var stop=link.getBusStop();
            return new TransitGraphStop(stop.getId(),stop.getExternalStopId(),stop.getArsNumber(),stop.getName(),
                    stop.getLatitude(),stop.getLongitude(),link.getStopSequence(),link.getDirection());
        }).toList();
        boolean continuous=true;
        for(int i=1;i<stops.size();i++)if(stops.get(i).sequence()!=stops.get(i-1).sequence()+1){continuous=false;break;}
        boolean allLocated=stops.stream().allMatch(TransitGraphStop::hasCoordinates);
        boolean repeated=stops.stream().map(TransitGraphStop::stopId).distinct().count()!=stops.size();
        return new TransitRouteGraph(route.getId(),route.getExternalRouteId(),route.getRouteNumber(),stops,continuous,allLocated,repeated);
    }
}
