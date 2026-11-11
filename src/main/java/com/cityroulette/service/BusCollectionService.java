package com.cityroulette.service;

import com.cityroulette.domain.ImportRun;
import com.cityroulette.dto.bus.*;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class BusCollectionService {
    private final BusanBusClient client; private final BusStopMapper stopMapper; private final BusRouteMapper routeMapper;
    private final BusRouteStopMapper routeStopMapper; private final BusImportService importer; private final ImportRunService runs;
    public BusCollectionService(BusanBusClient client,BusStopMapper stopMapper,BusRouteMapper routeMapper,BusRouteStopMapper routeStopMapper,BusImportService importer,ImportRunService runs){this.client=client;this.stopMapper=stopMapper;this.routeMapper=routeMapper;this.routeStopMapper=routeStopMapper;this.importer=importer;this.runs=runs;}

    public BusImportReport collectStops(boolean dryRun,int pageSize,int limit){
        validate(pageSize,limit); ImportRun run=dryRun?null:runs.start("BUSAN_BUS_STOP",limit); BusImportReport total=BusImportReport.empty("BUSAN_BUS_STOP",dryRun);
        try{int page=1,processed=0;while(processed<limit){var response=client.getStops(page,Math.min(pageSize,limit-processed),null,null);var mapped=new ArrayList<NormalizedBusStop>();int failures=0;for(var raw:response.items())try{mapped.add(stopMapper.map(raw));}catch(RuntimeException e){failures++;}
                var part=importer.importStops(mapped,dryRun);total=total.add(part.fetched(),part.inserted(),part.updated(),part.skipped(),part.failed()+failures,0);if(run!=null)runs.addPage(run.getId(),part.fetched(),part.inserted(),part.updated(),0,part.skipped(),part.failed()+failures);processed+=response.items().size();if(response.items().isEmpty()||processed>=response.totalCount())break;page++;}
            if(run!=null)runs.succeedOnly(run.getId());return total;
        }catch(RuntimeException e){if(run!=null)runs.fail(run.getId(),safe(e));throw e;}
    }

    public BusImportReport collectRoutes(boolean dryRun,List<String> routeNumbers,int routeLimit,int stopLimit){
        if(routeNumbers==null||routeNumbers.isEmpty())throw new IllegalArgumentException("노선 수집에는 route-numbers가 필요합니다.");
        ImportRun run=dryRun?null:runs.start("BUSAN_BUS_ROUTE",routeLimit);BusImportReport total=BusImportReport.empty("BUSAN_BUS_ROUTE",dryRun);Set<String> seen=new HashSet<>();
        try{outer:for(String number:routeNumbers)for(var raw:client.getRoutes(number)){if(raw.buslinenum()==null||!raw.buslinenum().trim().equalsIgnoreCase(number.trim()))continue;if(total.fetched()>=routeLimit)break outer;NormalizedBusRoute route;try{route=routeMapper.map(raw);}catch(RuntimeException e){total=total.add(1,0,0,0,1,0);continue;}if(!seen.add(route.externalRouteId())){total=total.add(1,0,0,1,0,0);continue;}
                    var links=new ArrayList<NormalizedBusRouteStop>();int failures=0;for(var linkRaw:client.getRouteStops(route.externalRouteId()).stream().limit(stopLimit).toList())try{links.add(routeStopMapper.map(linkRaw));}catch(RuntimeException e){failures++;}
                    var part=importer.importRoute(route,links,dryRun);total=total.add(part.fetched(),part.inserted(),part.updated(),part.skipped(),part.failed()+failures,part.routeStops());if(run!=null)runs.addPage(run.getId(),part.fetched(),part.inserted(),part.updated(),0,part.skipped(),part.failed()+failures);}
            if(run!=null)runs.succeedOnly(run.getId());return total;
        }catch(RuntimeException e){if(run!=null)runs.fail(run.getId(),safe(e));throw e;}
    }
    private void validate(int pageSize,int limit){if(pageSize<1||pageSize>100||limit<1)throw new IllegalArgumentException("page-size는 1~100, limit은 1 이상이어야 합니다.");}
    private String safe(RuntimeException e){return e.getMessage()==null?e.getClass().getSimpleName():e.getMessage();}
}
