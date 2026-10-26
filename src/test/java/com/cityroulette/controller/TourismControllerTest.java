package com.cityroulette.controller;

import com.cityroulette.dto.tourism.TourismPlaceDto;
import com.cityroulette.dto.tourism.TourismSyncResult;
import com.cityroulette.service.TourismImportService;
import com.cityroulette.service.TourismService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TourismControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TourismService tourismService;

    @MockitoBean
    private TourismImportService tourismImportService;

    @Test
    void usesDefaultPagination() throws Exception {
        when(tourismService.getBusanTouristPlaces(1, 10)).thenReturn(List.of());

        mockMvc.perform(get("/api/tourism/places"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        verify(tourismService).getBusanTouristPlaces(1, 10);
    }

    @Test
    void passesPageAndSizeParameters() throws Exception {
        TourismPlaceDto place = new TourismPlaceDto(
                "3017435", "12", "중리노을전망대", "부산광역시 영도구 동삼동", "632-3",
                "129.0643", "35.0692", "", "", "", "26", "200",
                "VE", "VE01", "VE010100", "", "49113", "", "");
        when(tourismService.getBusanTouristPlaces(2, 20)).thenReturn(List.of(place));

        mockMvc.perform(get("/api/tourism/places").param("page", "2").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].contentid").value("3017435"))
                .andExpect(jsonPath("$[0].title").value("중리노을전망대"))
                .andExpect(jsonPath("$[0].lDongRegnCd").value("26"));

        verify(tourismService).getBusanTouristPlaces(2, 20);
    }

    @ParameterizedTest
    @CsvSource({
            "0, 10",
            "1, 0",
            "1, 101"
    })
    void rejectsInvalidPagination(int page, int size) throws Exception {
        mockMvc.perform(get("/api/tourism/places")
                        .param("page", String.valueOf(page))
                        .param("size", String.valueOf(size)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(tourismService);
    }

    @Test
    void synchronizesOneRequestedPage() throws Exception {
        when(tourismImportService.syncBusanTouristPlaces(2, 20))
                .thenReturn(new TourismSyncResult(20, 15, 3, 2));

        mockMvc.perform(post("/api/tourism/sync").param("page", "2").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requested").value(20))
                .andExpect(jsonPath("$.inserted").value(15))
                .andExpect(jsonPath("$.updated").value(3))
                .andExpect(jsonPath("$.skipped").value(2));

        verify(tourismImportService).syncBusanTouristPlaces(2, 20);
    }

    @Test
    void rejectsInvalidSyncPagination() throws Exception {
        mockMvc.perform(post("/api/tourism/sync").param("page", "0"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(tourismImportService);
    }
}
