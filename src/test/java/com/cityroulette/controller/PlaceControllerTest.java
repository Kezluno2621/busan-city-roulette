package com.cityroulette.controller;

import com.cityroulette.domain.Place;
import com.cityroulette.service.PlaceService;
import org.junit.jupiter.api.Test;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PlaceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PlaceService placeService;

    @Test
    void returnsOnePageOfDatabasePlaces() throws Exception {
        when(placeService.getPlaces(2, 5)).thenReturn(List.of(new Place(
                "1997221", "감천문화마을", "관광지", "부산광역시 사하구",
                35.09, 129.01, true)));

        mockMvc.perform(get("/api/places").param("page", "2").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].externalId").value("1997221"))
                .andExpect(jsonPath("$[0].indoor").value(true));

        verify(placeService).getPlaces(2, 5);
    }

    @Test
    void rejectsInvalidPagination() throws Exception {
        mockMvc.perform(get("/api/places").param("size", "101"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(placeService);
    }
}
