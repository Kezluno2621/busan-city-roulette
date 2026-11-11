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

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DataVerificationServiceTest {
    @Autowired DataVerificationService service;
    @Autowired PlaceRepository places;
    @Autowired PlaceSourceRepository sources;
    @Autowired PlaceMergeCandidateRepository candidates;
    @Autowired BusStopRepository stops;
    @Autowired BusRouteRepository routes;
    @Autowired BusRouteStopRepository routeStops;

    @Test
    void reportsCountsMissingLinksAndNearbyMatchingRates(){
        candidates.deleteAll();sources.deleteAll();routeStops.deleteAll();routes.deleteAll();stops.deleteAll();places.deleteAll();
        Place located=places.save(new Place("MANUAL",null,"부산시청","관광지","부산 연제구","연제구",35.1799,129.0769,null,null));
        places.save(new Place("좌표 없는 장소","문화시설","부산",null,null,null));
        PlaceSource source=new PlaceSource(located,"TEST","P1");
        source.synchronize(null,null,null,LocalDateTime.now(),null,null,null);
        sources.save(source);
        BusStop stop=stops.save(new BusStop("S1","13708","부산시청",new BigDecimal("35.1800"),new BigDecimal("129.0770"),null));
        BusRoute linked=routes.save(new BusRoute("R1","100",null,null,null));
        routes.save(new BusRoute("R2","200",null,null,null));
        routeStops.save(new BusRouteStop(linked,stop,1,null));

        var result=service.snapshot();

        assertThat(result.places()).isEqualTo(2);
        assertThat(result.placeSources()).isEqualTo(1);
        assertThat(result.placesWithCoordinates()).isEqualTo(1);
        assertThat(result.busStops()).isEqualTo(1);
        assertThat(result.busStopsWithValidCoordinates()).isEqualTo(1);
        assertThat(result.busRoutes()).isEqualTo(2);
        assertThat(result.busRouteStops()).isEqualTo(1);
        assertThat(result.routesWithStops()).isEqualTo(1);
        assertThat(result.routesWithoutStops()).isEqualTo(1);
        assertThat(result.matchedWithin300Meters()).isEqualTo(100.0);
        assertThat(result.matchedWithin500Meters()).isEqualTo(100.0);
        assertThat(result.matchedWithin1000Meters()).isEqualTo(100.0);
    }
}
