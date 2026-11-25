package com.cityroulette.controller;

import com.cityroulette.dto.recommendation.RecommendationDto;
import com.cityroulette.dto.recommendation.RecommendationResponse;
import com.cityroulette.service.RecommendationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RecommendationControllerTest {

    private static final double LATITUDE = 35.1160;
    private static final double LONGITUDE = 128.9680;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RecommendationService recommendationService;

    @Test
    void usesDefaultStayAndLimitAndReturnsRecommendation() throws Exception {
        when(recommendationService.recommend(LATITUDE, LONGITUDE, 180, 60, 3))
                .thenReturn(RecommendationResponse.success(recommendation()));

        mockMvc.perform(get("/api/recommendations/random")
                        .param("lat", String.valueOf(LATITUDE))
                        .param("lon", String.valueOf(LONGITUDE))
                        .param("availableMinutes", "180"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommended").value(true))
                .andExpect(jsonPath("$.message").value("추천 장소를 찾았습니다."))
                .andExpect(jsonPath("$.recommendation.name").value("감천문화마을"))
                .andExpect(jsonPath("$.recommendation.oneWayTransitMinutes").value(30))
                .andExpect(jsonPath("$.recommendation.estimatedRoundTripMinutes").value(60))
                .andExpect(jsonPath("$.recommendation.estimatedRequiredMinutes").value(120));

        verify(recommendationService).recommend(LATITUDE, LONGITUDE, 180, 60, 3);
    }

    @Test
    void returnsNormalResponseWhenNoPlaceCanBeRecommended() throws Exception {
        when(recommendationService.recommend(LATITUDE, LONGITUDE, 100, 60, 1))
                .thenReturn(RecommendationResponse.notFound());

        mockMvc.perform(get("/api/recommendations/random")
                        .param("lat", String.valueOf(LATITUDE))
                        .param("lon", String.valueOf(LONGITUDE))
                        .param("availableMinutes", "100")
                        .param("stayMinutes", "60")
                        .param("limit", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommended").value(false))
                .andExpect(jsonPath("$.message").value(
                        "주어진 시간 안에 방문 가능한 관광지를 찾지 못했습니다."))
                .andExpect(jsonPath("$.recommendation").doesNotExist());
    }

    @ParameterizedTest
    @CsvSource({
            "91,129,180,60,3",
            "35,181,180,60,3",
            "NaN,129,180,60,3",
            "35,Infinity,180,60,3",
            "35,129,59,60,3",
            "35,129,721,60,3",
            "35,129,180,29,3",
            "35,129,180,241,3",
            "35,129,180,60,0",
            "35,129,180,60,6"
    })
    void rejectsInvalidParameters(
            String latitude,
            String longitude,
            String availableMinutes,
            String stayMinutes,
            String limit) throws Exception {
        mockMvc.perform(get("/api/recommendations/random")
                        .param("lat", latitude)
                        .param("lon", longitude)
                        .param("availableMinutes", availableMinutes)
                        .param("stayMinutes", stayMinutes)
                        .param("limit", limit))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(recommendationService);
    }

    private RecommendationDto recommendation() {
        return new RecommendationDto(
                10L,
                "1997221",
                "감천문화마을",
                "관광지",
                "부산광역시 사하구",
                35.0974606978,
                129.0105969965,
                4.3,
                30,
                60,
                60,
                120,
                180,
                620,
                1550,
                1,
                0,
                "출발 정류장",
                "도착 정류장",
                2);
    }
}
