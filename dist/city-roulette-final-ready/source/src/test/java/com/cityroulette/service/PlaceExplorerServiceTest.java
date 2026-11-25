package com.cityroulette.service;
import com.cityroulette.domain.*;
import com.cityroulette.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.assertThat;
@SpringBootTest @ActiveProfiles("test") @Transactional
class PlaceExplorerServiceTest {
    @Autowired PlaceExplorerService service;@Autowired PlaceRepository places;@Autowired PlaceSourceRepository sources;@Autowired BusStopRepository stops;@Autowired BusRouteRepository routes;@Autowired BusRouteStopRepository links;
    @Test void supportsCombinedFiltersAndPagination(){places.save(new Place("MANUAL",null,"해운대 카페","음식점","부산 해운대구","해운대구",35.16,129.16,null,null));places.save(new Place("MANUAL",null,"서면 카페","음식점","부산 부산진구","부산진구",35.15,129.05,null,null));places.save(new Place("MANUAL",null,"해운대 미술관","문화시설","부산 해운대구","해운대구",35.17,129.17,null,null));var combined=service.search("음식점","해운대구","카페",0,10);assertThat(combined.content()).singleElement().satisfies(x->assertThat(x.name()).isEqualTo("해운대 카페"));var page=service.search(null,null,null,0,1);assertThat(page.totalElements()).isGreaterThanOrEqualTo(3);assertThat(page.content()).hasSize(1);}
    @Test void combinesSourcesNearbyStopsAndRoutes(){Place place=places.save(new Place("MANUAL",null,"부산시청","관광지","부산 연제구","연제구",35.1799,129.0769,"설명",null));PlaceSource source=new PlaceSource(place,"BUSAN_ATTRACTION","1");source.synchronize("https://data.go.kr","ATTRACTION",null,LocalDateTime.now(),"부산광역시","제한 없음",null);sources.save(source);BusStop stop=stops.save(new BusStop("S1","13708","부산시청 정류소",new BigDecimal("35.1800"),new BigDecimal("129.0770"),"일반"));BusRoute route=routes.save(new BusRoute("R1","100","일반","기점","종점"));links.save(new BusRouteStop(route,stop,1,null));var detail=service.detail(place.getId(),500);assertThat(detail.sources()).singleElement().satisfies(x->assertThat(x.provider()).isEqualTo("BUSAN_ATTRACTION"));assertThat(detail.nearbyStops()).singleElement().satisfies(x->{assertThat(x.stop().name()).isEqualTo("부산시청 정류소");assertThat(x.routes()).extracting(r->r.routeNumber()).containsExactly("100");});assertThat(detail.mapAvailable()).isTrue();}
    @Test void handlesMissingCoordinatesAndTransportData(){Place place=places.save(new Place("좌표 없는 장소","관광지","부산",null,null,null));var detail=service.detail(place.getId(),500);assertThat(detail.mapAvailable()).isFalse();assertThat(detail.nearbyStops()).isEmpty();assertThat(detail.sources()).isEmpty();}
}
