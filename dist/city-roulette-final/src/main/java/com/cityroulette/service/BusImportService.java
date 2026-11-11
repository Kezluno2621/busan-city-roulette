package com.cityroulette.service;

import com.cityroulette.domain.*;
import com.cityroulette.dto.bus.*;
import com.cityroulette.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class BusImportService {
    private final BusStopRepository stops; private final BusRouteRepository routes; private final BusRouteStopRepository routeStops;
    public BusImportService(BusStopRepository stops,BusRouteRepository routes,BusRouteStopRepository routeStops){this.stops=stops;this.routes=routes;this.routeStops=routeStops;}

    @Transactional
    public BusImportReport importStops(List<NormalizedBusStop> items,boolean dryRun){
        BusImportReport result=BusImportReport.empty("BUSAN_BUS_STOP",dryRun); Set<String> seen=new HashSet<>();
        for(NormalizedBusStop item:items)try{
            if(item==null||item.externalStopId()==null||item.name()==null){result=result.add(1,0,0,1,0,0);continue;}
            if(!seen.add(item.externalStopId())){result=result.add(1,0,0,1,0,0);continue;}
            var existing=stops.findByExternalStopId(item.externalStopId());
            if(existing.isPresent()){if(!dryRun)existing.get().update(item.arsNumber(),item.name(),item.latitude(),item.longitude(),item.stopType());result=result.add(1,0,1,0,0,0);}
            else{if(!dryRun)stops.save(new BusStop(item.externalStopId(),item.arsNumber(),item.name(),item.latitude(),item.longitude(),item.stopType()));result=result.add(1,1,0,0,0,0);}
        }catch(RuntimeException e){result=result.add(1,0,0,0,1,0);}
        return result;
    }

    @Transactional
    public BusImportReport importRoute(NormalizedBusRoute item,List<NormalizedBusRouteStop> sequence,boolean dryRun){
        if(item==null)return BusImportReport.empty("BUSAN_BUS_ROUTE",dryRun).add(1,0,0,1,0,0);
        try{
            var existing=routes.findByExternalRouteId(item.externalRouteId()); boolean insert=existing.isEmpty();
            if(dryRun)return BusImportReport.empty("BUSAN_BUS_ROUTE",true).add(1,insert?1:0,insert?0:1,0,0,sequence.size());
            BusRoute route=existing.orElseGet(()->new BusRoute(item.externalRouteId(),item.routeNumber(),item.routeType(),item.startStopName(),item.endStopName()));
            if(existing.isPresent())route.update(item.routeNumber(),item.routeType(),item.startStopName(),item.endStopName());
            routes.save(route); int linked=0;
            for(NormalizedBusRouteStop link:sequence){
                BusStop stop=stops.findByExternalStopId(link.externalStopId()).orElseGet(()->stops.save(new BusStop(link.externalStopId(),link.arsNumber(),link.stopName(),link.latitude(),link.longitude(),null)));
                if(link.latitude()!=null&&link.longitude()!=null)stop.enrichCoordinates(link.latitude(),link.longitude(),link.arsNumber(),null);
                var current=routeStops.findByBusRouteIdAndStopSequence(route.getId(),link.sequence());
                if(current.isPresent())current.get().update(stop,link.direction()); else routeStops.save(new BusRouteStop(route,stop,link.sequence(),link.direction()));
                linked++;
            }
            return BusImportReport.empty("BUSAN_BUS_ROUTE",false).add(1,insert?1:0,insert?0:1,0,0,linked);
        }catch(RuntimeException e){return BusImportReport.empty("BUSAN_BUS_ROUTE",dryRun).add(1,0,0,0,1,0);}
    }

    @Transactional
    public boolean enrichStopCoordinates(String targetExternalId,NormalizedBusStop evidence,boolean dryRun){
        if(targetExternalId==null||evidence==null||evidence.latitude()==null||evidence.longitude()==null)return false;
        BusStop target=stops.findByExternalStopId(targetExternalId).orElseThrow(()->new IllegalArgumentException("보강 대상 정류장을 찾을 수 없습니다."));
        if(dryRun)return true;
        target.enrichCoordinates(evidence.latitude(),evidence.longitude(),evidence.arsNumber(),evidence.stopType());
        return true;
    }
}
