package com.cityroulette.service;

import com.cityroulette.domain.BusRouteStop;
import com.cityroulette.dto.bus.*;
import com.cityroulette.repository.BusRouteStopRepository;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly=true)
public class TransitRoutePlannerService {
    private static final int MAX_ROUTE_GRAPHS=200;
    private static final int MAX_RAW_CANDIDATES=2_000;
    private static final TransitDataCoverageStatus COVERAGE=TransitDataCoverageStatus.PARTIAL_DATA;
    private static final String WALKING_TYPE="HAVERSINE_STRAIGHT_LINE_ACCESS_AND_EGRESS_NOT_WALKING_ROUTE";
    private final BusQueryService busQueries;
    private final BusRouteStopRepository routeStops;
    private final TransitGraphService graphs;

    public TransitRoutePlannerService(BusQueryService busQueries,BusRouteStopRepository routeStops,TransitGraphService graphs){
        this.busQueries=busQueries;this.routeStops=routeStops;this.graphs=graphs;
    }

    public TransitRoutePlanResponse plan(Long originPlaceId,Long destinationPlaceId,int radiusMeters,int maxCandidates){
        if(originPlaceId==null||destinationPlaceId==null)throw new IllegalArgumentException("출발지와 목적지가 필요합니다.");
        if(radiusMeters<50||radiusMeters>3_000)throw new IllegalArgumentException("검색 반경은 50~3000m이어야 합니다.");
        if(maxCandidates<1||maxCandidates>20)throw new IllegalArgumentException("후보 수는 1~20이어야 합니다.");
        NearbyBusStopResponse origin=busQueries.nearby(originPlaceId,radiusMeters);
        if(originPlaceId.equals(destinationPlaceId))throw new IllegalArgumentException("출발지와 목적지는 달라야 합니다.");
        NearbyBusStopResponse destination=busQueries.nearby(destinationPlaceId,radiusMeters);
        if(origin.stops().isEmpty()||destination.stops().isEmpty())return empty(originPlaceId,destinationPlaceId,radiusMeters);

        Map<Long,Double> originDistances=distances(origin);
        Map<Long,Double> destinationDistances=distances(destination);
        List<BusRouteStop> originLinks=routeStops.findWithRoutesByBusStopIds(originDistances.keySet());
        List<BusRouteStop> destinationLinks=routeStops.findWithRoutesByBusStopIds(destinationDistances.keySet());
        Set<Long> originRouteIds=routeIds(originLinks);
        Set<Long> destinationRouteIds=routeIds(destinationLinks);
        LinkedHashSet<Long> requestedRoutes=new LinkedHashSet<>();
        originRouteIds.stream().sorted().limit(MAX_ROUTE_GRAPHS).forEach(requestedRoutes::add);
        destinationRouteIds.stream().sorted().filter(id->requestedRoutes.size()<MAX_ROUTE_GRAPHS).forEach(requestedRoutes::add);
        Map<Long,TransitRouteGraph> graphByRoute=graphs.routes(requestedRoutes);

        List<TransitRouteCandidate> found=new ArrayList<>();
        Set<String> keys=new HashSet<>();
        direct(originPlaceId,destinationPlaceId,originDistances,destinationDistances,originRouteIds,destinationRouteIds,graphByRoute,found,keys);
        transfers(originPlaceId,destinationPlaceId,originDistances,destinationDistances,originRouteIds,destinationRouteIds,graphByRoute,found,keys);
        Comparator<TransitRouteCandidate> order=Comparator.comparingInt(TransitRouteCandidate::transferCount)
                .thenComparingDouble(TransitRouteCandidate::walkingDistanceMeters)
                .thenComparingInt(TransitRouteCandidate::stopCount)
                .thenComparing(x->String.join(",",x.busRouteExternalIds()));
        List<TransitRouteCandidate> candidates=found.stream().sorted(order).limit(maxCandidates).toList();
        if(candidates.isEmpty())return empty(originPlaceId,destinationPlaceId,radiusMeters);
        return new TransitRoutePlanResponse(originPlaceId,destinationPlaceId,radiusMeters,
                TransitVerificationStatus.VERIFIED,COVERAGE,
                "VERIFIED는 현재 적재된 연속 정차 구간에만 적용되며 부산 전체 노선망의 완전성을 의미하지 않습니다.",
                WALKING_TYPE,candidates);
    }

    private void direct(Long originPlaceId,Long destinationPlaceId,Map<Long,Double> originDistances,
            Map<Long,Double> destinationDistances,Set<Long> originRoutes,Set<Long> destinationRoutes,
            Map<Long,TransitRouteGraph> graphByRoute,List<TransitRouteCandidate> result,Set<String> keys){
        for(Long routeId:originRoutes){
            if(!destinationRoutes.contains(routeId))continue;
            TransitRouteGraph graph=graphByRoute.get(routeId);if(graph==null)continue;
            List<TransitGraphStop> stops=graph.stops();
            for(int from=0;from<stops.size();from++){
                TransitGraphStop boarding=stops.get(from);if(!originDistances.containsKey(boarding.stopId()))continue;
                for(int to=from+1;to<stops.size();to++){
                    TransitGraphStop alighting=stops.get(to);if(!destinationDistances.containsKey(alighting.stopId()))continue;
                    if(!continuous(stops,from,to))continue;
                    String key="D:"+routeId+":"+boarding.sequence()+":"+alighting.sequence();
                    if(keys.add(key)&&result.size()<MAX_RAW_CANDIDATES)result.add(candidate(originPlaceId,destinationPlaceId,
                            TransitRouteType.DIRECT,boarding,alighting,null,List.of(graph),to-from,
                            originDistances.get(boarding.stopId())+destinationDistances.get(alighting.stopId())));
                }
            }
        }
    }

    private void transfers(Long originPlaceId,Long destinationPlaceId,Map<Long,Double> originDistances,
            Map<Long,Double> destinationDistances,Set<Long> originRoutes,Set<Long> destinationRoutes,
            Map<Long,TransitRouteGraph> graphByRoute,List<TransitRouteCandidate> result,Set<String> keys){
        for(Long firstId:originRoutes)for(Long secondId:destinationRoutes){
            if(firstId.equals(secondId)||result.size()>=MAX_RAW_CANDIDATES)continue;
            TransitRouteGraph first=graphByRoute.get(firstId),second=graphByRoute.get(secondId);
            if(first==null||second==null)continue;
            Map<Long,List<Integer>> secondOccurrences=occurrences(second.stops());
            for(int board=0;board<first.stops().size();board++){
                TransitGraphStop boarding=first.stops().get(board);if(!originDistances.containsKey(boarding.stopId()))continue;
                for(int transferOut=board+1;transferOut<first.stops().size();transferOut++){
                    if(!continuous(first.stops(),board,transferOut))break;
                    TransitGraphStop transfer=first.stops().get(transferOut);
                    for(int transferIn:secondOccurrences.getOrDefault(transfer.stopId(),List.of())){
                        for(int alight=transferIn+1;alight<second.stops().size();alight++){
                            TransitGraphStop destination=second.stops().get(alight);
                            if(!destinationDistances.containsKey(destination.stopId()))continue;
                            if(!continuous(second.stops(),transferIn,alight))continue;
                            String key="T:"+firstId+":"+boarding.sequence()+":"+transfer.sequence()+":"+secondId+":"+second.stops().get(transferIn).sequence()+":"+destination.sequence();
                            if(keys.add(key)&&result.size()<MAX_RAW_CANDIDATES)result.add(candidate(originPlaceId,destinationPlaceId,
                                    TransitRouteType.ONE_TRANSFER,boarding,destination,transfer,List.of(first,second),
                                    (transferOut-board)+(alight-transferIn),
                                    originDistances.get(boarding.stopId())+destinationDistances.get(destination.stopId())));
                        }
                    }
                }
            }
        }
    }

    private TransitRouteCandidate candidate(Long originPlaceId,Long destinationPlaceId,TransitRouteType type,
            TransitGraphStop boarding,TransitGraphStop alighting,TransitGraphStop transfer,
            List<TransitRouteGraph> routes,int stopCount,double walking){
        return new TransitRouteCandidate(originPlaceId,destinationPlaceId,type,type==TransitRouteType.DIRECT?0:1,
                summary(boarding),summary(alighting),transfer==null?null:summary(transfer),
                routes.stream().map(TransitRouteGraph::routeNumber).toList(),
                routes.stream().map(TransitRouteGraph::externalRouteId).toList(),stopCount,round(walking),
                TransitVerificationStatus.VERIFIED,COVERAGE);
    }
    private TransitRoutePlanResponse empty(Long origin,Long destination,int radius){
        return new TransitRoutePlanResponse(origin,destination,radius,TransitVerificationStatus.NO_CANDIDATE,COVERAGE,
                "현재 적재된 부분 데이터 안에서만 후보가 없습니다. 부산 전체 버스망에서 이동 불가능하다는 뜻이 아닙니다.",
                WALKING_TYPE,List.of());
    }
    private Map<Long,Double> distances(NearbyBusStopResponse response){return response.stops().stream().collect(Collectors.toMap(x->x.stop().id(),NearbyBusStopResponse.NearbyStop::distanceMeters,Math::min,LinkedHashMap::new));}
    private Set<Long> routeIds(List<BusRouteStop> links){return links.stream().map(x->x.getBusRoute().getId()).collect(Collectors.toCollection(LinkedHashSet::new));}
    private Map<Long,List<Integer>> occurrences(List<TransitGraphStop> stops){Map<Long,List<Integer>> result=new HashMap<>();for(int i=0;i<stops.size();i++)result.computeIfAbsent(stops.get(i).stopId(),ignored->new ArrayList<>()).add(i);return result;}
    private boolean continuous(List<TransitGraphStop> stops,int from,int to){for(int i=from+1;i<=to;i++)if(stops.get(i).sequence()!=stops.get(i-1).sequence()+1)return false;return true;}
    private TransitStopSummary summary(TransitGraphStop stop){return new TransitStopSummary(stop.stopId(),stop.externalStopId(),stop.arsNumber(),stop.name(),stop.latitude(),stop.longitude());}
    private double round(double value){return Math.round(value*10.0)/10.0;}
}
