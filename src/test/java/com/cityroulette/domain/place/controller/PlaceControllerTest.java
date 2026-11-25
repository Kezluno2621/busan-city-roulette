package com.cityroulette.domain.place.controller;

import com.cityroulette.domain.place.dto.PlacePageResponse;
import com.cityroulette.domain.place.dto.PlaceResponse;
import com.cityroulette.domain.place.service.PlaceService;
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
        PlaceResponse place = new PlaceResponse(
                1L, "감천문화마을", "관광지", "부산광역시 사하구", "사하구",
                35.09, 129.01, null, "KOR_SERVICE", "1997221", true, null, null, null);
        when(placeService.searchPlaces(null, null, null, 2, 5))
                .thenReturn(new PlacePageResponse(List.of(place), 2, 5, 1, 1, false, true));

        mockMvc.perform(get("/api/places").param("page", "2").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].externalId").value("1997221"))
                .andExpect(jsonPath("$.content[0].indoor").value(true))
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.size").value(5));

        verify(placeService).searchPlaces(null, null, null, 2, 5);
    }

    @Test
    void rejectsInvalidPagination() throws Exception {
        mockMvc.perform(get("/api/places").param("size", "101"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(placeService);
    }
}
