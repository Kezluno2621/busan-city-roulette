package com.cityroulette.service;
import com.cityroulette.dto.bus.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
class BusMappersTest {
    @Test void mapsStopCoordinates(){var mapped=new BusStopMapper().map(new BusStopRaw("167970102","01001"," 영주삼거리 ","129.03332177581","35.115355761619","일반"));assertThat(mapped.externalStopId()).isEqualTo("167970102");assertThat(mapped.latitude().toPlainString()).isEqualTo("35.1153558");}
    @Test void removesInvalidCoordinates(){var mapped=new BusStopMapper().map(new BusStopRaw("1",null,"정류소","127","37",null));assertThat(mapped.latitude()).isNull();assertThat(mapped.longitude()).isNull();}
    @Test void mapsRouteAndSequence(){var route=new BusRouteMapper().map(new BusRouteRaw("5200100000","100","일반버스","기점","종점"));var stop=new BusRouteStopMapper().map(new BusRouteStopRaw("517010000","1","청강리공영차고지","16419","기점","35.115355761619","129.03332177581"));assertThat(route.routeNumber()).isEqualTo("100");assertThat(stop.externalStopId()).isEqualTo("517010000");assertThat(stop.sequence()).isEqualTo(1);assertThat(stop.direction()).isEqualTo("기점");assertThat(stop.latitude()).isEqualByComparingTo("35.1153558");}
}
