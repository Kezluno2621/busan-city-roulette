package com.cityroulette.domain.transport.controller;

import com.cityroulette.domain.transport.dto.BusPageResponse;
import com.cityroulette.domain.transport.dto.BusRouteResponse;
import com.cityroulette.domain.transport.dto.BusRouteStopResponse;
import com.cityroulette.domain.transport.dto.BusStopResponse;
import com.cityroulette.domain.transport.service.BusQueryService;
import com.cityroulette.domain.transport.service.BusResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class BusControllerTest {
    @Autowired MockMvc mvc; @MockitoBean BusQueryService service;
    @Test void exposesPublicPaginatedStopsAndRouteStops() throws Exception {
        when(service.stops(null,0,20)).thenReturn(new BusPageResponse<>(List.of(new BusStopResponse(1L,"S1","01001","정류소",null,null,"일반")),0,20,1,1));
        mvc.perform(get("/api/bus/stops")).andExpect(status().isOk()).andExpect(jsonPath("$.content[0].externalStopId").value("S1"));
        when(service.routeStops(1L)).thenReturn(List.of(new BusRouteStopResponse(1,null,new BusStopResponse(1L,"S1",null,"정류소",null,null,null))));
        mvc.perform(get("/api/bus/routes/1/stops")).andExpect(status().isOk()).andExpect(jsonPath("$[0].stopSequence").value(1));
        when(service.stopRoutes(1L)).thenReturn(List.of(new BusRouteResponse(2L,"R1","100","일반","기점","종점")));
        mvc.perform(get("/api/bus/stops/1/routes")).andExpect(status().isOk()).andExpect(jsonPath("$[0].routeNumber").value("100"));
    }
    @Test void returns404AndValidatesNearbyRadius() throws Exception {
        when(service.stop(999L)).thenThrow(new BusResourceNotFoundException("정류소",999L));
        mvc.perform(get("/api/bus/stops/999")).andExpect(status().isNotFound());
        mvc.perform(get("/api/places/1/nearby-bus-stops").param("radius","5000")).andExpect(status().isBadRequest());
    }
}
