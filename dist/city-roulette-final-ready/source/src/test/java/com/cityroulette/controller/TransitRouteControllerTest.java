package com.cityroulette.controller;

import com.cityroulette.dto.bus.*;
import com.cityroulette.service.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TransitRouteControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean TransitRoutePlannerService planner;

    @Test void exposesPublicRoutePlanDto()throws Exception{
        when(planner.plan(1L,2L,1000,10)).thenReturn(new TransitRoutePlanResponse(1L,2L,1000,
                TransitVerificationStatus.NO_CANDIDATE,TransitDataCoverageStatus.PARTIAL_DATA,
                "현재 데이터 내 후보 없음","HAVERSINE",List.of()));

        mvc.perform(get("/api/transit/routes").param("originPlaceId","1").param("destinationPlaceId","2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.originPlaceId").value(1))
                .andExpect(jsonPath("$.verificationStatus").value("NO_CANDIDATE"))
                .andExpect(jsonPath("$.dataCoverageStatus").value("PARTIAL_DATA"))
                .andExpect(jsonPath("$.candidates").isArray());
    }

    @Test void validatesParametersAndMapsPlaceErrors()throws Exception{
        mvc.perform(get("/api/transit/routes").param("originPlaceId","1"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/transit/routes").param("originPlaceId","1").param("destinationPlaceId","2").param("limit","21"))
                .andExpect(status().isBadRequest());
        when(planner.plan(999L,2L,1000,10)).thenThrow(new PlaceNotFoundException(999L));
        mvc.perform(get("/api/transit/routes").param("originPlaceId","999").param("destinationPlaceId","2"))
                .andExpect(status().isNotFound());
    }
}
