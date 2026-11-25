package com.cityroulette.domain.place.controller;

import com.cityroulette.domain.place.dto.PlaceDetailView;
import com.cityroulette.domain.place.dto.PlacePageResponse;
import com.cityroulette.domain.place.dto.PlaceResponse;
import com.cityroulette.domain.place.service.PlaceExplorerService;
import com.cityroulette.domain.place.service.PlaceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PlaceExplorerControllerTest {
    @Autowired
    private MockMvc mvc;
    @MockitoBean
    private PlaceExplorerService service;

    @Test
    void publicListPreservesCombinedFiltersAndRendersCards() throws Exception {
        when(service.search("관광지", "사하구", "감천", 0, 12))
                .thenReturn(new PlacePageResponse(List.of(place()), 0, 12, 1, 1, true, true));
        when(service.categories()).thenReturn(List.of("관광지"));
        when(service.districts()).thenReturn(List.of("사하구"));

        mvc.perform(get("/places")
                        .param("keyword", "감천")
                        .param("category", "관광지")
                        .param("district", "사하구"))
                .andExpect(status().isOk())
                .andExpect(view().name("place/list"))
                .andExpect(content().string(containsString("감천문화마을")));
        verify(service).search("관광지", "사하구", "감천", 0, 12);
    }

    @Test
    void rendersEmptyListAndDetailWithoutLogin() throws Exception {
        when(service.search(null, null, null, 0, 12))
                .thenReturn(new PlacePageResponse(List.of(), 0, 12, 0, 0, true, true));
        when(service.categories()).thenReturn(List.of());
        when(service.districts()).thenReturn(List.of());

        mvc.perform(get("/places"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("조건에 맞는 장소가 없습니다")));

        PlaceResponse place = place();
        when(service.detail(1L, 500))
                .thenReturn(new PlaceDetailView(place, List.of(), place.updatedAt(), List.of(), true));

        mvc.perform(get("/places/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("place/detail"))
                .andExpect(content().string(containsString("data-place-map")));
    }

    @Test
    void missingPlaceUses404Page() throws Exception {
        when(service.detail(999L, 500)).thenThrow(new PlaceNotFoundException(999L));

        mvc.perform(get("/places/999"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/404"))
                .andExpect(content().string(containsString("여행지 목록으로")));
    }

    @Test
    void paginationWindowStaysCompactAndCentered() {
        assertThat(PlaceExplorerController.pageWindow(0, 33)).containsExactly(0, 1, 2, 3, 4);
        assertThat(PlaceExplorerController.pageWindow(16, 33)).containsExactly(14, 15, 16, 17, 18);
        assertThat(PlaceExplorerController.pageWindow(32, 33)).containsExactly(28, 29, 30, 31, 32);
        assertThat(PlaceExplorerController.pageWindow(0, 0)).isEmpty();
    }

    private PlaceResponse place() {
        return new PlaceResponse(1L, "감천문화마을", "관광지", "부산 사하구", "사하구",
                35.09, 129.01, "설명", "BUSAN_ATTRACTION", "1", null, null, null,
                "https://example.com/a.jpg");
    }
}
