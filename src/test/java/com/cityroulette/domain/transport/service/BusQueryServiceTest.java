package com.cityroulette.domain.transport.service;

import com.cityroulette.domain.place.entity.Place;
import com.cityroulette.domain.place.repository.PlaceRepository;
import com.cityroulette.domain.place.service.PlaceCoordinatesMissingException;
import com.cityroulette.domain.place.service.PlaceNotFoundException;
import com.cityroulette.domain.transport.entity.BusRoute;
import com.cityroulette.domain.transport.entity.BusRouteStop;
import com.cityroulette.domain.transport.entity.BusStop;
import com.cityroulette.domain.transport.repository.BusRouteRepository;
import com.cityroulette.domain.transport.repository.BusRouteStopRepository;
import com.cityroulette.domain.transport.repository.BusStopRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import java.math.BigDecimal;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:bus-query-fixture;MODE=MySQL;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test")
class BusQueryServiceTest {
    @Autowired BusQueryService service;@Autowired BusStopRepository stops;@Autowired BusRouteRepository routes;@Autowired BusRouteStopRepository links;@Autowired PlaceRepository places;
    @BeforeEach void clean(){links.deleteAll();routes.deleteAll();stops.deleteAll();places.deleteAll();}
    @Test void findsNearbyStopsInDistanceOrder(){Place place=places.save(new Place("관광지","관광지","부산",35.1799,129.0769,null));stops.save(new BusStop("S1","13708","가까운 정류소",new BigDecimal("35.1800"),new BigDecimal("129.0770"),"일반"));stops.save(new BusStop("S2",null,"먼 정류소",new BigDecimal("35.1900"),new BigDecimal("129.0900"),"일반"));var result=service.nearby(place.getId(),500);assertThat(result.distanceType()).contains("NOT_WALKING_DISTANCE");assertThat(result.stops()).singleElement().satisfies(x->assertThat(x.stop().name()).isEqualTo("가까운 정류소"));}
    @Test void haversineDistanceIsReasonable(){assertThat(service.distance(35.1799,129.0769,35.1809,129.0769)).isBetween(110.0,112.5);}
    @Test void rejectsMissingPlaceAndMissingCoordinates(){assertThatThrownBy(()->service.nearby(999L,500)).isInstanceOf(PlaceNotFoundException.class);Place noCoordinates=places.save(new Place("좌표없음","관광지","부산",null,null,null));assertThatThrownBy(()->service.nearby(noCoordinates.getId(),500)).isInstanceOf(PlaceCoordinatesMissingException.class);}
    @Test void bulkLoadsRoutesForMultipleStops(){BusStop first=stops.save(new BusStop("S10",null,"첫 번째",new BigDecimal("35.1800"),new BigDecimal("129.0770"),null));BusStop second=stops.save(new BusStop("S11",null,"둘째",new BigDecimal("35.1810"),new BigDecimal("129.0780"),null));BusRoute route=routes.save(new BusRoute("R10","100",null,null,null));links.save(new BusRouteStop(route,first,1,null));links.save(new BusRouteStop(route,second,2,null));var result=service.routesByStopIds(List.of(first.getId(),second.getId()));assertThat(result).containsOnlyKeys(first.getId(),second.getId());assertThat(result.get(first.getId())).extracting(r->r.routeNumber()).containsExactly("100");assertThat(result.get(second.getId())).extracting(r->r.routeNumber()).containsExactly("100");}
}
