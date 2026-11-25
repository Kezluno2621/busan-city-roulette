package com.cityroulette.ingestion.bus;

import com.cityroulette.integration.ExternalApiException;
import com.cityroulette.integration.bus.BusanBusClient;
import com.cityroulette.ingestion.history.ImportRunRepository;
import com.cityroulette.domain.transport.repository.BusRouteRepository;
import com.cityroulette.domain.transport.repository.BusRouteStopRepository;
import com.cityroulette.domain.transport.repository.BusStopRepository;
import com.cityroulette.integration.bus.dto.BusRouteRaw;
import com.cityroulette.integration.bus.dto.BusRouteStopRaw;
import java.util.List;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
@SpringBootTest @ActiveProfiles("test")
class BusCollectionServiceTest {
    @Autowired BusCollectionService service;@Autowired ImportRunRepository runs;@Autowired BusRouteStopRepository routeStops;@Autowired BusRouteRepository routes;@Autowired BusStopRepository stops;@MockitoBean BusanBusClient client;
    @BeforeEach void clean(){routeStops.deleteAll();routes.deleteAll();stops.deleteAll();runs.deleteAll();}
    @Test void recordsFailureOnlyForRealImport(){when(client.getStops(1,10,null,null)).thenThrow(new ExternalApiException("버스 호출 실패"));assertThatThrownBy(()->service.collectStops(false,10,10)).isInstanceOf(ExternalApiException.class);assertThat(runs.findAll()).singleElement().satisfies(x->{assertThat(x.getStatus()).isEqualTo("FAILED");assertThat(x.getErrorMessage()).isEqualTo("버스 호출 실패");});runs.deleteAll();assertThatThrownBy(()->service.collectStops(true,10,10)).isInstanceOf(ExternalApiException.class);assertThat(runs.count()).isZero();}
    @Test void importsOnlyTheExactRouteNumberFromSearchResults(){when(client.getRoutes("100")).thenReturn(List.of(new BusRouteRaw("R1","100-1",null,null,null),new BusRouteRaw("R2","100",null,null,null)));when(client.getRouteStops("R2")).thenReturn(List.of(new BusRouteStopRaw("S1","1","첫 정류장",null,null,null,null)));var result=service.collectRoutes(true,List.of("100"),1,20);assertThat(result.fetched()).isEqualTo(1);assertThat(result.routeStops()).isEqualTo(1);verify(client,never()).getRouteStops("R1");verify(client).getRouteStops("R2");}
}
