package com.cityroulette.ingestion.bus;

import com.cityroulette.domain.transport.dto.TransitGraphStop;
import com.cityroulette.domain.transport.entity.BusRoute;
import com.cityroulette.domain.transport.entity.BusRouteStop;
import com.cityroulette.domain.transport.entity.BusStop;
import com.cityroulette.domain.transport.repository.BusRouteRepository;
import com.cityroulette.domain.transport.repository.BusRouteStopRepository;
import com.cityroulette.domain.transport.repository.BusStopRepository;
import com.cityroulette.domain.transport.service.TransitGraphService;
import com.cityroulette.integration.bus.BusanBusClient;
import com.cityroulette.integration.bus.dto.BusApiPage;
import com.cityroulette.integration.bus.dto.BusStopRaw;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class BusStopEnrichmentServiceTest {
    @Autowired BusStopEnrichmentService service;
    @Autowired TransitGraphService graphService;
    @Autowired BusStopRepository stops;
    @Autowired BusRouteRepository routes;
    @Autowired BusRouteStopRepository links;
    @MockitoBean BusanBusClient client;

    @BeforeEach void clean(){links.deleteAll();routes.deleteAll();stops.deleteAll();}

    @Test void enrichesBySameOfficialIdAndRemainsIdempotent(){
        Fixture fixture=fixture("NODE-1","10001","첫 정류장");
        when(client.getStops(1,20,null,"10001")).thenReturn(page(raw("NODE-1","10001","첫 정류장")));

        var first=service.enrichRoute(fixture.route().getId(),20,false);
        var second=service.enrichRoute(fixture.route().getId(),20,false);

        assertThat(first.matchedByExternalId()).isEqualTo(1);
        assertThat(first.enriched()).isEqualTo(1);
        assertThat(second.targets()).isZero();
        assertThat(stops.count()).isEqualTo(1);
        assertThat(stops.findByExternalStopId("NODE-1").orElseThrow().getLatitude()).isEqualByComparingTo("35.1153558");
        var graph=graphService.route(fixture.route().getId());
        assertThat(graph.allStopsLocated()).isTrue();
        assertThat(graph.sequenceContinuous()).isTrue();
    }

    @Test void uniqueArsAndSameNameMayEnrichButKeepsIdentifierKindsDistinct(){
        Fixture fixture=fixture("ROUTE-NODE-1","10002","둘째 정류장");
        when(client.getStops(1,20,null,"10002")).thenReturn(page(raw("MASTER-STOP-9","10002","둘째 정류장")));

        var report=service.enrichRoute(fixture.route().getId(),20,false);

        assertThat(report.matchedByArsNumber()).isEqualTo(1);
        assertThat(stops.findByExternalStopId("ROUTE-NODE-1")).isPresent();
        assertThat(stops.findByExternalStopId("MASTER-STOP-9")).isEmpty();
        assertThat(stops.count()).isEqualTo(1);
    }

    @Test void ambiguousArsIsReviewCandidateAndIsNotMerged(){
        Fixture fixture=fixture("NODE-3","10003","같은 정류장");
        when(client.getStops(1,20,null,"10003")).thenReturn(new BusApiPage<>(List.of(
                raw("MASTER-A","10003","같은 정류장"),raw("MASTER-B","10003","같은 정류장")),1,20,2));

        var report=service.enrichRoute(fixture.route().getId(),20,false);

        assertThat(report.unresolved()).isEqualTo(1);
        assertThat(report.reviewCandidates()).isEqualTo(1);
        assertThat(stops.findByExternalStopId("NODE-3").orElseThrow().getLatitude()).isNull();
        assertThat(stops.count()).isEqualTo(1);
    }

    @Test void duplicatedArsInDatabaseBlocksFallbackMerge(){
        Fixture fixture=fixture("NODE-DB-A","10005","DB 중복 정류장");
        stops.save(new BusStop("NODE-DB-B","10005","다른 정류장",null,null,null));
        when(client.getStops(1,20,null,"10005")).thenReturn(page(raw("MASTER-ONLY","10005","DB 중복 정류장")));

        var report=service.enrichRoute(fixture.route().getId(),20,false);

        assertThat(report.enriched()).isZero();
        assertThat(report.reviewCandidates()).isEqualTo(1);
        assertThat(stops.findByExternalStopId("NODE-DB-A").orElseThrow().getLatitude()).isNull();
    }

    @Test void nameOnlyCandidateIsNeverMerged(){
        Fixture fixture=fixture("NODE-4",null,"이름 전용 후보");
        when(client.getStops(1,20,"이름 전용 후보",null)).thenReturn(page(raw("OTHER-ID","20004","이름 전용 후보")));

        var report=service.enrichRoute(fixture.route().getId(),20,false);

        assertThat(report.enriched()).isZero();
        assertThat(report.reviewCandidates()).isEqualTo(1);
        assertThat(report.issues()).singleElement().satisfies(x->assertThat(x.reason()).isEqualTo("NAME_ONLY_NOT_MERGED"));
        assertThat(stops.count()).isEqualTo(1);
    }

    @Test void graphPreservesOrderDirectionAndRepeatedStopReferences(){
        BusRoute route=routes.save(new BusRoute("R-LOOP","100","일반",null,null));
        BusStop stop=stops.save(new BusStop("LOOP-STOP","30001","순환 정류장",new BigDecimal("35.1"),new BigDecimal("129.1"),null));
        links.save(new BusRouteStop(route,stop,1,"상행"));
        links.save(new BusRouteStop(route,stop,2,"하행"));

        var graph=graphService.route(route.getId());

        assertThat(graph.stops()).extracting(TransitGraphStop::sequence).containsExactly(1,2);
        assertThat(graph.stops()).extracting(TransitGraphStop::direction).containsExactly("상행","하행");
        assertThat(graph.repeatedStops()).isTrue();
        assertThat(graph.sequenceContinuous()).isTrue();
    }

    private Fixture fixture(String id,String ars,String name){
        BusRoute route=routes.save(new BusRoute("R-"+id,"100",null,null,null));
        BusStop stop=stops.save(new BusStop(id,ars,name,null,null,null));
        links.save(new BusRouteStop(route,stop,1,null));
        return new Fixture(route,stop);
    }
    private BusStopRaw raw(String id,String ars,String name){return new BusStopRaw(id,ars,name,"129.03332177581","35.115355761619","일반");}
    private BusApiPage<BusStopRaw> page(BusStopRaw raw){return new BusApiPage<>(List.of(raw),1,20,1);}
    private record Fixture(BusRoute route,BusStop stop){ }
}
