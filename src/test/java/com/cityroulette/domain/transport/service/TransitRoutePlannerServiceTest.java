package com.cityroulette.domain.transport.service;

import com.cityroulette.domain.place.entity.Place;
import com.cityroulette.domain.place.repository.PlaceRepository;
import com.cityroulette.domain.place.service.PlaceCoordinatesMissingException;
import com.cityroulette.domain.place.service.PlaceNotFoundException;
import com.cityroulette.domain.transport.dto.TransitDataCoverageStatus;
import com.cityroulette.domain.transport.dto.TransitRouteType;
import com.cityroulette.domain.transport.dto.TransitVerificationStatus;
import com.cityroulette.domain.transport.entity.BusRoute;
import com.cityroulette.domain.transport.entity.BusRouteStop;
import com.cityroulette.domain.transport.entity.BusStop;
import com.cityroulette.domain.transport.repository.BusRouteRepository;
import com.cityroulette.domain.transport.repository.BusRouteStopRepository;
import com.cityroulette.domain.transport.repository.BusStopRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:transit-plan-fixture;MODE=MySQL;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test")
class TransitRoutePlannerServiceTest {
    @Autowired TransitRoutePlannerService planner;
    @Autowired TransitGraphService graphs;
    @Autowired PlaceRepository places;
    @Autowired BusStopRepository stops;
    @Autowired BusRouteRepository routes;
    @Autowired BusRouteStopRepository links;

    @BeforeEach void clean(){links.deleteAll();routes.deleteAll();stops.deleteAll();places.deleteAll();}

    @Test void findsVerifiedDirectRoute(){
        BusStop origin=stop("O",35.1000,129.1000),destination=stop("D",35.2000,129.2000);
        route("R1","100",link(origin,1),link(destination,2));
        Place from=place("출발",35.1000,129.1000),to=place("도착",35.2000,129.2000);

        var result=planner.plan(from.getId(),to.getId(),200,10);

        assertThat(result.verificationStatus()).isEqualTo(TransitVerificationStatus.VERIFIED);
        assertThat(result.dataCoverageStatus()).isEqualTo(TransitDataCoverageStatus.PARTIAL_DATA);
        assertThat(result.candidates()).singleElement().satisfies(candidate->{
            assertThat(candidate.routeType()).isEqualTo(TransitRouteType.DIRECT);
            assertThat(candidate.transferCount()).isZero();
            assertThat(candidate.busRouteNumbers()).containsExactly("100");
            assertThat(candidate.stopCount()).isEqualTo(1);
        });
    }

    @Test void rejectsReverseDirectionOnSameStoredRoute(){
        BusStop first=stop("FIRST",35.1000,129.1000),second=stop("SECOND",35.2000,129.2000);
        route("R2","101",link(first,1),link(second,2));
        Place from=place("역방향출발",35.2000,129.2000),to=place("역방향도착",35.1000,129.1000);

        assertThat(planner.plan(from.getId(),to.getId(),200,10).verificationStatus())
                .isEqualTo(TransitVerificationStatus.NO_CANDIDATE);
    }

    @Test void findsOneTransferAtSameOfficialStop(){
        BusStop origin=stop("TO",35.1000,129.1000),transfer=stop("TX",35.1500,129.1500),destination=stop("TD",35.2000,129.2000);
        route("RA","10",link(origin,1),link(transfer,2));
        route("RB","20",link(transfer,1),link(destination,2));
        Place from=place("환승출발",35.1000,129.1000),to=place("환승도착",35.2000,129.2000);

        var result=planner.plan(from.getId(),to.getId(),200,10);

        assertThat(result.candidates()).singleElement().satisfies(candidate->{
            assertThat(candidate.routeType()).isEqualTo(TransitRouteType.ONE_TRANSFER);
            assertThat(candidate.transferCount()).isEqualTo(1);
            assertThat(candidate.transferStop().externalStopId()).isEqualTo("TX");
            assertThat(candidate.busRouteNumbers()).containsExactly("10","20");
        });
    }

    @Test void doesNotTransferBetweenDifferentStopsWithOnlySameName(){
        BusStop origin=stop("NO",35.1000,129.1000),firstTransfer=stop("NX1",35.1500,129.1500);
        BusStop secondTransfer=stop("NX2",35.1500,129.1500),destination=stop("ND",35.2000,129.2000);
        route("NRA","30",link(origin,1),link(firstTransfer,2));
        route("NRB","40",link(secondTransfer,1),link(destination,2));
        Place from=place("불가출발",35.1000,129.1000),to=place("불가도착",35.2000,129.2000);

        assertThat(planner.plan(from.getId(),to.getId(),200,10).candidates()).isEmpty();
    }

    @Test void evaluatesEachRepeatedStopOccurrenceBySequence(){
        BusStop origin=stop("LOOP-O",35.1000,129.1000),middle=stop("LOOP-M",35.1500,129.1500),destination=stop("LOOP-D",35.2000,129.2000);
        BusRoute route=route("LOOP-R","50",link(origin,1),link(middle,2),link(origin,3),link(destination,4));
        Place from=place("반복출발",35.1000,129.1000),to=place("반복도착",35.2000,129.2000);

        var result=planner.plan(from.getId(),to.getId(),200,10);

        assertThat(graphs.route(route.getId()).repeatedStops()).isTrue();
        assertThat(result.candidates()).isNotEmpty();
        assertThat(result.candidates().get(0).stopCount()).isEqualTo(1);
    }

    @Test void supportsForwardSegmentOfCircularRoute(){
        BusStop loop=stop("C-O",35.1000,129.1000),origin=stop("C-A",35.1500,129.1500),destination=stop("C-D",35.2000,129.2000);
        BusRoute route=route("C-R","60",link(loop,1),link(origin,2),link(destination,3),link(loop,4));
        Place from=place("순환출발",35.1500,129.1500),to=place("순환도착",35.1000,129.1000);

        var result=planner.plan(from.getId(),to.getId(),200,10);

        assertThat(graphs.route(route.getId()).repeatedStops()).isTrue();
        assertThat(result.candidates()).singleElement().satisfies(x->assertThat(x.stopCount()).isEqualTo(2));
    }

    @Test void rejectsSegmentWithMissingSequence(){
        BusStop origin=stop("G-O",35.1000,129.1000),destination=stop("G-D",35.2000,129.2000);
        route("G-R","70",link(origin,1),link(destination,3));
        Place from=place("누락출발",35.1000,129.1000),to=place("누락도착",35.2000,129.2000);

        assertThat(planner.plan(from.getId(),to.getId(),200,10).candidates()).isEmpty();
    }

    @Test void acceptsContinuousPartialStoredSegmentButMarksCoveragePartial(){
        BusStop origin=stop("P-O",35.1000,129.1000),destination=stop("P-D",35.2000,129.2000);
        route("P-R","80",link(origin,10),link(destination,11));
        Place from=place("부분출발",35.1000,129.1000),to=place("부분도착",35.2000,129.2000);

        var result=planner.plan(from.getId(),to.getId(),200,10);

        assertThat(result.candidates()).singleElement().satisfies(x->{
            assertThat(x.verificationStatus()).isEqualTo(TransitVerificationStatus.VERIFIED);
            assertThat(x.dataCoverageStatus()).isEqualTo(TransitDataCoverageStatus.PARTIAL_DATA);
        });
    }

    @Test void rejectsPlaceWithoutCoordinates(){
        Place origin=places.save(new Place("좌표없음","관광지","부산",null,null,null));
        Place destination=place("정상",35.2,129.2);
        assertThatThrownBy(()->planner.plan(origin.getId(),destination.getId(),500,10))
                .isInstanceOf(PlaceCoordinatesMissingException.class);
    }

    @Test void rejectsSameOriginAndDestination(){
        Place place=place("동일",35.1,129.1);
        assertThatThrownBy(()->planner.plan(place.getId(),place.getId(),500,10))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test void rejectsUnknownPlace(){
        Place destination=place("정상",35.2,129.2);
        assertThatThrownBy(()->planner.plan(999999L,destination.getId(),500,10))
                .isInstanceOf(PlaceNotFoundException.class);
    }

    @Test void limitsAndSortsCandidates(){
        BusStop origin=stop("L-O",35.1000,129.1000),destination=stop("L-D",35.2000,129.2000);
        route("L-R1","91",link(origin,1),link(destination,2));
        route("L-R2","92",link(origin,1),link(destination,2));
        route("L-R3","93",link(origin,1),link(destination,2));
        Place from=place("제한출발",35.1000,129.1000),to=place("제한도착",35.2000,129.2000);

        assertThat(planner.plan(from.getId(),to.getId(),200,2).candidates()).hasSize(2);
    }

    @Test void reportsNoCandidateAsPartialDataNotGlobalImpossibility(){
        stop("ONLY-O",35.1000,129.1000);stop("ONLY-D",35.2000,129.2000);
        Place from=place("데이터출발",35.1000,129.1000),to=place("데이터도착",35.2000,129.2000);

        var result=planner.plan(from.getId(),to.getId(),200,10);

        assertThat(result.verificationStatus()).isEqualTo(TransitVerificationStatus.NO_CANDIDATE);
        assertThat(result.dataCoverageStatus()).isEqualTo(TransitDataCoverageStatus.PARTIAL_DATA);
        assertThat(result.interpretation()).contains("전체 버스망");
    }

    private Place place(String name,double latitude,double longitude){return places.save(new Place(name,"관광지","부산",latitude,longitude,null));}
    private BusStop stop(String id,double latitude,double longitude){return stops.save(new BusStop(id,null,id,new BigDecimal(String.valueOf(latitude)),new BigDecimal(String.valueOf(longitude)),null));}
    private StopLink link(BusStop stop,int sequence){return new StopLink(stop,sequence);}
    private BusRoute route(String externalId,String number,StopLink... sequence){
        BusRoute route=routes.save(new BusRoute(externalId,number,"일반",null,null));
        for(StopLink item:sequence)links.save(new BusRouteStop(route,item.stop(),item.sequence(),"FORWARD"));
        return route;
    }
    private record StopLink(BusStop stop,int sequence){ }
}
