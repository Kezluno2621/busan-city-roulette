package com.cityroulette.controller;

import com.cityroulette.dto.place.TransitPlaceCandidateDto;
import com.cityroulette.service.PlaceTransitEvaluationService;
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
class PlaceTransitCandidateControllerTest {

    private static final double LATITUDE = 35.1160;
    private static final double LONGITUDE = 128.9680;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PlaceTransitEvaluationService evaluationService;

    @Test
    void usesDefaultLimitOfThreeAndReturnsEvaluatedCandidate() throws Exception {
        when(evaluationService.evaluateCandidates(LATITUDE, LONGITUDE, 3))
                .thenReturn(List.of(candidate()));

        mockMvc.perform(get("/api/places/transit-candidates")
                        .param("lat", String.valueOf(LATITUDE))
                        .param("lon", String.valueOf(LONGITUDE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("감천문화마을"))
                .andExpect(jsonPath("$[0].totalTime").value(31))
                .andExpect(jsonPath("$[0].totalWalk").value(620))
                .andExpect(jsonPath("$[0].payment").value(1550))
                .andExpect(jsonPath("$[0].transitAvailable").value(true));

        verify(evaluationService).evaluateCandidates(LATITUDE, LONGITUDE, 3);
    }

    @Test
    void returnsEmptyArrayWhenThereAreNoCandidates() throws Exception {
        when(evaluationService.evaluateCandidates(LATITUDE, LONGITUDE, 2))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/places/transit-candidates")
                        .param("lat", String.valueOf(LATITUDE))
                        .param("lon", String.valueOf(LONGITUDE))
                        .param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @ParameterizedTest
    @CsvSource({
            "91,129,3",
            "-91,129,3",
            "35,181,3",
            "35,-181,3",
            "NaN,129,3",
            "35,Infinity,3",
            "35,129,0",
            "35,129,6"
    })
    void rejectsInvalidCoordinatesAndLimit(String latitude, String longitude, String limit)
            throws Exception {
        mockMvc.perform(get("/api/places/transit-candidates")
                        .param("lat", latitude)
                        .param("lon", longitude)
                        .param("limit", limit))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(evaluationService);
    }

    private TransitPlaceCandidateDto candidate() {
        return new TransitPlaceCandidateDto(
                10L,
                "1997221",
                "감천문화마을",
                "관광지",
                "부산광역시 사하구",
                35.0974606978,
                129.0105969965,
                4.3,
                31,
                620,
                1550,
                1,
                0,
                "출발 정류장",
                "도착 정류장",
                2,
                true);
    }
}
