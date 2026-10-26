package com.cityroulette.controller;

import com.cityroulette.dto.location.LocationSearchResultDto;
import com.cityroulette.dto.recommendation.RecommendationDto;
import com.cityroulette.dto.recommendation.RecommendationResponse;
import com.cityroulette.service.LocationSearchService;
import com.cityroulette.service.RecommendationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RecommendationPageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RecommendationService recommendationService;

    @MockitoBean
    private LocationSearchService locationSearchService;

    @Test
    void displaysRecommendationFormWithSafeDefaults() throws Exception {
        mockMvc.perform(get("/recommend"))
                .andExpect(status().isOk())
                .andExpect(view().name("recommend"))
                .andExpect(model().attribute("availableMinutes", 180))
                .andExpect(model().attribute("stayMinutes", 60))
                .andExpect(model().attribute("limit", 1))
                .andExpect(content().string(containsString("어디로 떠나볼까요?")))
                .andExpect(content().string(containsString("부산역, 동아대학교 승학캠퍼스")))
                .andExpect(content().string(containsString("룰렛 돌리기")));

        verifyNoInteractions(recommendationService);
        verifyNoInteractions(locationSearchService);
    }

    @Test
    void searchesDeparturePlacesAndRendersSelectableResults() throws Exception {
        LocationSearchResultDto busanStation = new LocationSearchResultDto(
                "101", "부산역", "교통,수송 > 기차역", "부산광역시 동구 중앙대로 206",
                35.1151, 129.0414);
        when(locationSearchService.search("부산역")).thenReturn(List.of(busanStation));

        mockMvc.perform(post("/recommend/search-location")
                        .param("locationQuery", " 부산역 ")
                        .param("availableMinutes", "240")
                        .param("stayMinutes", "90")
                        .param("limit", "2")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("recommend"))
                .andExpect(model().attribute("locationSearchResults", List.of(busanStation)))
                .andExpect(model().attribute("availableMinutes", "240"))
                .andExpect(content().string(containsString("부산광역시 동구 중앙대로 206")))
                .andExpect(content().string(containsString("data-latitude=\"35.1151\"")))
                .andExpect(content().string(containsString("data-longitude=\"129.0414\"")));

        verify(locationSearchService).search("부산역");
        verifyNoInteractions(recommendationService);
    }

    @Test
    void callsRecommendationServiceOnceAndRendersSuccessfulResult() throws Exception {
        RecommendationResponse response = RecommendationResponse.success(recommendation());
        when(recommendationService.recommend(35.1160, 128.9680, 180, 60, 1))
                .thenReturn(response);

        mockMvc.perform(post("/recommend")
                        .param("locationQuery", "감천문화마을")
                        .param("departureName", "감천문화마을 입구")
                        .param("latitude", "35.1160")
                        .param("longitude", "128.9680")
                        .param("availableMinutes", "180")
                        .param("stayMinutes", "60")
                        .param("limit", "1")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("recommend"))
                .andExpect(model().attribute("recommendationResponse", response))
                .andExpect(model().attribute("departureName", "감천문화마을 입구"))
                .andExpect(model().attribute("latitude", "35.1160"))
                .andExpect(model().attribute("longitude", "128.9680"))
                .andExpect(content().string(containsString("감천문화마을")))
                .andExpect(content().string(containsString("35</b>분")))
                .andExpect(content().string(containsString("130</b>분")));

        verify(recommendationService).recommend(35.1160, 128.9680, 180, 60, 1);
    }

    @Test
    void rendersNoRecommendationWithoutServerError() throws Exception {
        RecommendationResponse response = RecommendationResponse.notFound();
        when(recommendationService.recommend(35.1160, 128.9680, 120, 60, 1))
                .thenReturn(response);

        mockMvc.perform(post("/recommend")
                        .param("departureName", "부산역")
                        .param("latitude", "35.1160")
                        .param("longitude", "128.9680")
                        .param("availableMinutes", "120")
                        .param("stayMinutes", "60")
                        .param("limit", "1")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("recommend"))
                .andExpect(model().attribute("recommendationResponse", response))
                .andExpect(content().string(containsString("조건에 맞는 관광지를 찾지 못했어요.")))
                .andExpect(content().string(containsString("다시 조건 입력하기")));

        verify(recommendationService).recommend(35.1160, 128.9680, 120, 60, 1);
    }

    @Test
    void rendersInputErrorForInvalidLatitudeWithoutCallingService() throws Exception {
        mockMvc.perform(post("/recommend")
                        .param("departureName", "잘못된 위치")
                        .param("latitude", "200")
                        .param("longitude", "128.9680")
                        .param("availableMinutes", "180")
                        .param("stayMinutes", "60")
                        .param("limit", "1")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("recommend"))
                .andExpect(model().attributeExists("inputError"))
                .andExpect(model().attribute("latitude", "200"))
                .andExpect(content().string(containsString("위도 정보가 올바르지 않습니다")));

        verifyNoInteractions(recommendationService);
    }

    @Test
    void rendersInputErrorForInvalidLongitudeWithoutCallingService() throws Exception {
        mockMvc.perform(post("/recommend")
                        .param("departureName", "잘못된 위치")
                        .param("latitude", "35.1160")
                        .param("longitude", "not-a-number")
                        .param("availableMinutes", "180")
                        .param("stayMinutes", "60")
                        .param("limit", "1")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("recommend"))
                .andExpect(model().attributeExists("inputError"))
                .andExpect(model().attribute("longitude", "not-a-number"))
                .andExpect(content().string(containsString("경도 정보가 올바르지 않습니다")));

        verifyNoInteractions(recommendationService);
    }

    @Test
    void asksUserToSelectSearchResultBeforeRecommendation() throws Exception {
        mockMvc.perform(post("/recommend")
                        .param("locationQuery", "부산역")
                        .param("availableMinutes", "180")
                        .param("stayMinutes", "60")
                        .param("limit", "1")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("recommend"))
                .andExpect(model().attributeExists("inputError"))
                .andExpect(content().string(containsString("검색한 뒤 결과에서 한 곳을 선택")));

        verifyNoInteractions(recommendationService);
    }

    @Test
    void homePageLinksToRecommendationPage() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("href=\"/recommend\"")))
                .andExpect(content().string(containsString("룰렛 돌리기")));
    }

    private RecommendationDto recommendation() {
        return new RecommendationDto(
                10L,
                "1997221",
                "감천문화마을",
                "관광지",
                "부산광역시 사하구 감내2로 203",
                35.0974606978,
                129.0105969965,
                4.3,
                35,
                70,
                60,
                130,
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
