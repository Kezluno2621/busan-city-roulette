package com.cityroulette.ingestion.bus;

import com.cityroulette.domain.transport.repository.BusRouteRepository;
import com.cityroulette.domain.transport.repository.BusRouteStopRepository;
import com.cityroulette.domain.transport.repository.BusStopRepository;
import com.cityroulette.ingestion.bus.dto.NormalizedBusRoute;
import com.cityroulette.ingestion.bus.dto.NormalizedBusRouteStop;
import com.cityroulette.ingestion.bus.dto.NormalizedBusStop;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import java.math.BigDecimal;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
@SpringBootTest @ActiveProfiles("test")
class BusImportServiceTest {
    @Autowired BusImportService service;@Autowired BusStopRepository stops;@Autowired BusRouteRepository routes;@Autowired BusRouteStopRepository links;
    @BeforeEach void clean(){links.deleteAll();routes.deleteAll();stops.deleteAll();}
    @Test void stopUpsertIsIdempotent(){var first=service.importStops(List.of(stop("1","첫 이름")),false);var second=service.importStops(List.of(stop("1","새 이름")),false);assertThat(first.inserted()).isEqualTo(1);assertThat(second.updated()).isEqualTo(1);assertThat(stops.count()).isEqualTo(1);assertThat(stops.findAll().get(0).getName()).isEqualTo("새 이름");}
    @Test void routeAndOrderedStopsDoNotDuplicate(){var route=new NormalizedBusRoute("R1","100","일반","기점","종점");var sequence=List.of(new NormalizedBusRouteStop("S1","10001","정류소1",1,null),new NormalizedBusRouteStop("S1","10001","정류소1",2,"2"));service.importRoute(route,sequence,false);service.importRoute(route,sequence,false);assertThat(routes.count()).isEqualTo(1);assertThat(stops.count()).isEqualTo(1);assertThat(links.count()).isEqualTo(2);assertThat(links.findByBusRouteIdOrderByStopSequence(routes.findAll().get(0).getId())).extracting(x->x.getStopSequence()).containsExactly(1,2);}
    @Test void dryRunChangesNothing(){var report=service.importStops(List.of(stop("2","미리보기")),true);var routeReport=service.importRoute(new NormalizedBusRoute("R2","200",null,null,null),List.of(new NormalizedBusRouteStop("S2",null,"정류소",1,null)),true);assertThat(report.inserted()).isEqualTo(1);assertThat(routeReport.inserted()).isEqualTo(1);assertThat(stops.count()).isZero();assertThat(routes.count()).isZero();assertThat(links.count()).isZero();}
    private NormalizedBusStop stop(String id,String name){return new NormalizedBusStop(id,"01001",name,new BigDecimal("35.1153558"),new BigDecimal("129.0333218"),"일반");}
}
