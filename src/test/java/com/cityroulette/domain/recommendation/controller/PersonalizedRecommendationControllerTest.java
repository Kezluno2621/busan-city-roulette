package com.cityroulette.domain.recommendation.controller;

import com.cityroulette.domain.recommendation.dto.PersonalizedRecommendationResponse;
import com.cityroulette.domain.recommendation.service.PersonalizedRecommendationService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PersonalizedRecommendationControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean PersonalizedRecommendationService recommendations;

    @Test void exposesPublicPostApiWithDtoResponse() throws Exception {
        when(recommendations.recommend(any())).thenReturn(new PersonalizedRecommendationResponse(
                List.of(), 0, true, "후보 없음", List.of("부분 데이터")));

        mvc.perform(post("/api/recommendations").with(csrf()).contentType("application/json")
                        .content("""
                                {"originLatitude":35.1,"originLongitude":129.1,"randomness":50,"limit":10}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidateCount").value(0))
                .andExpect(jsonPath("$.insufficientCandidates").value(true));
    }

    @Test void rejectsInvalidRangesBeforeServiceCall() throws Exception {
        mvc.perform(post("/api/recommendations").with(csrf()).contentType("application/json")
                        .content("""
                                {"originLatitude":91,"originLongitude":129.1,"randomness":101,"limit":21}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}
