package com.cityroulette.controller;

import com.cityroulette.dto.course.CourseGenerationResponse;
import com.cityroulette.dto.course.CourseOriginDto;
import com.cityroulette.service.CourseGenerationService;
import java.time.Instant;
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
class CourseGenerationControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean CourseGenerationService courses;

    @Test void returnsGeneratedCourseDto() throws Exception {
        when(courses.generate(any())).thenReturn(new CourseGenerationResponse("부산 즉흥 여행 코스 · 0곳",
                new CourseOriginDto(null, "출발지", 35.1, 129.1), Instant.parse("2026-10-29T00:00:00Z"),
                3, 0, List.of(), true, "후보 부족", List.of("부분 데이터"), "save-token"));

        mvc.perform(post("/api/courses/generate").with(csrf()).contentType("application/json")
                        .content("""
                                {"originLatitude":35.1,"originLongitude":129.1,"randomness":60,"placeCount":3}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestedPlaceCount").value(3))
                .andExpect(jsonPath("$.actualPlaceCount").value(0))
                .andExpect(jsonPath("$.places").isArray());
    }

    @Test void rejectsInvalidRequestAndRequiresCsrf() throws Exception {
        String invalid = """
                {"originLatitude":35.1,"originLongitude":129.1,"randomness":101,"placeCount":6}
                """;
        mvc.perform(post("/api/courses/generate").with(csrf()).contentType("application/json").content(invalid))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
        mvc.perform(post("/api/courses/generate").contentType("application/json").content(invalid))
                .andExpect(status().isForbidden());
    }
}
