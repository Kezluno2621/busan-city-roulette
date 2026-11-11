package com.cityroulette.service;

import com.cityroulette.domain.BusStop;
import com.cityroulette.dto.bus.*;
import com.cityroulette.repository.BusRouteStopRepository;
import com.cityroulette.repository.BusStopRepository;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class BusStopEnrichmentService {
    private final BusRouteStopRepository routeStops;
    private final BusanBusClient client;
    private final BusStopMapper mapper;
    private final BusImportService importer;
    private final BusStopRepository stops;

    public BusStopEnrichmentService(BusRouteStopRepository routeStops,BusanBusClient client,
            BusStopMapper mapper,BusImportService importer,BusStopRepository stops){
        this.routeStops=routeStops;this.client=client;this.mapper=mapper;this.importer=importer;this.stops=stops;
    }

    public BusStopEnrichmentReport enrichRoute(Long routeId,int limit,boolean dryRun){
        if(routeId==null)throw new IllegalArgumentException("노선 ID가 필요합니다.");
        if(limit<1||limit>100)throw new IllegalArgumentException("보강 제한은 1~100이어야 합니다.");
        List<BusStop> targets=routeStops.findStopsMissingCoordinates(routeId).stream().limit(limit).toList();
        int external=0,ars=0,enriched=0,unresolved=0,review=0,failed=0;
        List<BusStopEnrichmentIssue> issues=new ArrayList<>();
        for(BusStop target:targets){
            try{
                Match match=lookup(target);
                if(match.type==MatchType.EXTERNAL_ID)external++;
                else if(match.type==MatchType.ARS_NUMBER)ars++;
                else{
                    unresolved++;
                    if(match.type==MatchType.REVIEW_CANDIDATE)review++;
                    issues.add(new BusStopEnrichmentIssue(target.getExternalStopId(),target.getArsNumber(),match.reason));
                    continue;
                }
                if(importer.enrichStopCoordinates(target.getExternalStopId(),match.stop,dryRun))enriched++;
            }catch(RuntimeException exception){
                failed++;
                issues.add(new BusStopEnrichmentIssue(target.getExternalStopId(),target.getArsNumber(),"API_OR_MAPPING_FAILURE"));
            }
        }
        return new BusStopEnrichmentReport(dryRun,targets.size(),external,ars,enriched,unresolved,review,failed,List.copyOf(issues));
    }

    private Match lookup(BusStop target){
        List<BusStopRaw> candidates=new ArrayList<>();
        if(target.getArsNumber()!=null){
            BusApiPage<BusStopRaw> page=client.getStops(1,20,null,target.getArsNumber());
            if(page!=null&&page.items()!=null)candidates.addAll(page.items());
        }
        Optional<NormalizedBusStop> exact=valid(candidates).filter(x->target.getExternalStopId().equals(x.externalStopId())).findFirst();
        if(exact.isPresent())return new Match(MatchType.EXTERNAL_ID,exact.get(),"OFFICIAL_ID");

        if(candidates.isEmpty()||target.getArsNumber()==null){
            BusApiPage<BusStopRaw> page=client.getStops(1,20,target.getName(),null);
            if(page!=null&&page.items()!=null)candidates.addAll(page.items());
            exact=valid(candidates).filter(x->target.getExternalStopId().equals(x.externalStopId())).findFirst();
            if(exact.isPresent())return new Match(MatchType.EXTERNAL_ID,exact.get(),"OFFICIAL_ID");
        }

        if(target.getArsNumber()!=null){
            List<NormalizedBusStop> arsMatches=valid(candidates)
                    .filter(x->same(target.getArsNumber(),x.arsNumber())).toList();
            boolean uniqueInDatabase=stops.findAllByArsNumber(target.getArsNumber()).stream()
                    .allMatch(x->x.getId().equals(target.getId()));
            if(arsMatches.size()==1&&uniqueInDatabase&&sameName(target.getName(),arsMatches.get(0).name()))
                return new Match(MatchType.ARS_NUMBER,arsMatches.get(0),"UNIQUE_ARS_AND_NAME");
            if(!arsMatches.isEmpty())return new Match(MatchType.REVIEW_CANDIDATE,null,"AMBIGUOUS_ARS");
        }
        boolean sameName=valid(candidates).anyMatch(x->sameName(target.getName(),x.name()));
        return new Match(sameName?MatchType.REVIEW_CANDIDATE:MatchType.UNRESOLVED,null,
                sameName?"NAME_ONLY_NOT_MERGED":"NO_VERIFIED_MATCH");
    }

    private java.util.stream.Stream<NormalizedBusStop> valid(List<BusStopRaw> raw){
        return raw.stream().map(item->{try{return mapper.map(item);}catch(RuntimeException e){return null;}})
                .filter(Objects::nonNull).filter(x->x.latitude()!=null&&x.longitude()!=null);
    }
    private boolean same(String a,String b){return a!=null&&b!=null&&a.trim().equalsIgnoreCase(b.trim());}
    private boolean sameName(String a,String b){return normalize(a).equals(normalize(b));}
    private String normalize(String value){return value==null?"":value.replaceAll("[^0-9A-Za-z가-힣]","").toLowerCase(Locale.ROOT);}
    private enum MatchType{EXTERNAL_ID,ARS_NUMBER,REVIEW_CANDIDATE,UNRESOLVED}
    private record Match(MatchType type,NormalizedBusStop stop,String reason){ }
}
