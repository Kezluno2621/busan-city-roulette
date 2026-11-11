package com.cityroulette.controller;

import com.cityroulette.dto.place.NearbyPlaceDto;
import com.cityroulette.service.PlaceCandidateService;
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
class PlaceCandidateControllerTest {

    private static final double LATITUDE = 35.1160;
    private static final double LONGITUDE = 128.9680;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PlaceCandidateService placeCandidateService;

    @Test
    void returnsNearbyPlacesWithDefaultLimit() throws Exception {
        NearbyPlaceDto place = new NearbyPlaceDto(
                10L,
                "1997221",
                "감천문화마을",
                "관광지",
                "부산광역시 사하구",
                35.0974606978,
                129.0105969965,
                4.3);
        when(placeCandidateService.findNearestPlaces(LATITUDE, LONGITUDE, 5))
                .thenReturn(List.of(place));

        mockMvc.perform(get("/api/places/nearby")
                        .param("lat", String.valueOf(LATITUDE))
                        .param("lon", String.valueOf(LONGITUDE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].externalId").value("1997221"))
                .andExpect(jsonPath("$[0].name").value("감천문화마을"))
                .andExpect(jsonPath("$[0].distanceKm").value(4.3));

        verify(placeCandidateService).findNearestPlaces(LATITUDE, LONGITUDE, 5);
    }

    @Test
    void returnsEmptyArrayWhenNoPlacesExist() throws Exception {
        when(placeCandidateService.findNearestPlaces(LATITUDE, LONGITUDE, 5))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/places/nearby")
                        .param("lat", String.valueOf(LATITUDE))
                        .param("lon", String.valueOf(LONGITUDE)))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @ParameterizedTest
    @CsvSource({
            "91,129,5",
            "-91,129,5",
            "35,181,5",
            "35,-181,5",
            "NaN,129,5",
            "35,Infinity,5",
            "35,129,0",
            "35,129,11"
    })
    void rejectsInvalidCoordinatesAndLimit(String latitude, String longitude, String limit)
            throws Exception {
        mockMvc.perform(get("/api/places/nearby")
                        .param("lat", latitude)
                        .param("lon", longitude)
                        .param("limit", limit))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(placeCandidateService);
    }
}
