package com.cityroulette.controller;

import com.cityroulette.dto.transit.TransitRouteDto;
import com.cityroulette.service.TransitService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TransitControllerTest {

    private static final double SX = 129.0105969965;
    private static final double SY = 35.0974606978;
    private static final double EX = 129.123566;
    private static final double EY = 35.115449;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransitService transitService;

    @Test
    void returnsRoutesFromOneServiceCall() throws Exception {
        when(transitService.findRoutes(SX, SY, EX, EY))
                .thenReturn(List.of(route(82)));

        mockMvc.perform(validRequest("/api/transit/routes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].pathType").value(2))
                .andExpect(jsonPath("$[0].totalTime").value(82))
                .andExpect(jsonPath("$[0].totalWalk").value(1182))
                .andExpect(jsonPath("$[0].payment").value(1550))
                .andExpect(jsonPath("$[0].firstStartStation").value("출발 정류장"))
                .andExpect(jsonPath("$[0].lastEndStation").value("도착 정류장"));

        verify(transitService).findRoutes(SX, SY, EX, EY);
    }

    @Test
    void returnsFastestRoute() throws Exception {
        when(transitService.findFastestRoute(SX, SY, EX, EY))
                .thenReturn(Optional.of(route(61)));

        mockMvc.perform(validRequest("/api/transit/fastest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTime").value(61));

        verify(transitService).findFastestRoute(SX, SY, EX, EY);
    }

    @Test
    void returnsNotFoundWhenFastestRouteDoesNotExist() throws Exception {
        when(transitService.findFastestRoute(SX, SY, EX, EY))
                .thenReturn(Optional.empty());

        mockMvc.perform(validRequest("/api/transit/fastest"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(""));
    }

    @ParameterizedTest
    @CsvSource({
            "181,35,129,35",
            "129,91,129,35",
            "129,35,-181,35",
            "129,35,129,-91",
            "NaN,35,129,35",
            "129,Infinity,129,35"
    })
    void rejectsInvalidCoordinates(String sx, String sy, String ex, String ey) throws Exception {
        mockMvc.perform(get("/api/transit/routes")
                        .param("sx", sx)
                        .param("sy", sy)
                        .param("ex", ex)
                        .param("ey", ey))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(transitService);
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder validRequest(
            String path) {
        return get(path)
                .param("sx", String.valueOf(SX))
                .param("sy", String.valueOf(SY))
                .param("ex", String.valueOf(EX))
                .param("ey", String.valueOf(EY));
    }

    private TransitRouteDto route(int totalTime) {
        return new TransitRouteDto(
                2, totalTime, 1182, 1550, 1, 0,
                "출발 정류장", "도착 정류장");
    }
}
