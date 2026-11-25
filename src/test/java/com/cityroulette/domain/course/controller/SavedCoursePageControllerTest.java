package com.cityroulette.domain.course.controller;

import com.cityroulette.domain.course.dto.SavedCoursePageResponse;
import com.cityroulette.domain.course.dto.SavedCoursePlaceResponse;
import com.cityroulette.domain.course.dto.SavedCourseResponse;
import com.cityroulette.domain.course.dto.SavedCourseSummaryResponse;
import com.cityroulette.domain.course.dto.CourseEstimateStatus;
import com.cityroulette.domain.member.dto.MemberProfile;
import com.cityroulette.domain.member.service.MemberService;
import com.cityroulette.domain.course.service.SavedCourseService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class SavedCoursePageControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean SavedCourseService courses;
    @MockitoBean MemberService members;

    @BeforeEach void profile(){when(members.getProfile("owner")).thenReturn(new MemberProfile("owner","여행자","owner@test.local"));}

    @Test void guestIsRedirectedAndMemberCanRenderList() throws Exception {
        mvc.perform(get("/my/courses")).andExpect(status().is3xxRedirection());
        var summary = new SavedCourseSummaryResponse(7L,"부산 코스",2,"첫 장소",LocalDateTime.now(),LocalDateTime.now(),0L);
        when(courses.list("owner",0,12,"")).thenReturn(new SavedCoursePageResponse(List.of(summary),0,12,1,1));
        mvc.perform(get("/my/courses").with(user("owner"))).andExpect(status().isOk())
                .andExpect(view().name("course/list")).andExpect(content().string(containsString("부산 코스")))
                .andExpect(content().string(containsString("첫 장소")));
    }

    @Test void rendersOwnedDetailAndSnapshotMapData() throws Exception {
        var place = new SavedCoursePlaceResponse(3L,1,"감천문화마을","관광지","부산",35.1,129.1,
                88.5,List.of("추천 이유"),120.0,com.cityroulette.domain.recommendation.dto.RecommendationTransitStatus.PARTIAL_DATA,List.of());
        var response = new SavedCourseResponse(7L,"부산 코스","출발",35.09,129.09,1,1,60,180,30000L,
                CourseEstimateStatus.UNKNOWN,CourseEstimateStatus.UNKNOWN,List.of("직선거리"),List.of(place),
                LocalDateTime.now(),LocalDateTime.now(),0L);
        when(courses.findOne("owner",7L)).thenReturn(response);
        mvc.perform(get("/my/courses/7").with(user("owner"))).andExpect(status().isOk())
                .andExpect(view().name("course/detail"))
                .andExpect(content().string(containsString("감천문화마을")))
                .andExpect(content().string(containsString("data-course-place")))
                .andExpect(content().string(containsString("data-delete-dialog")))
                .andExpect(content().string(containsString("실제 도로·도보·버스 이동 경로가 아닙니다")));
    }

    @Test void staleVersionRendersCourseWithConflictGuidance() throws Exception {
        var response = new SavedCourseResponse(7L,"최신 제목","출발",35.09,129.09,1,1,60,180,30000L,
                CourseEstimateStatus.UNKNOWN,CourseEstimateStatus.UNKNOWN,List.of(),List.of(),
                LocalDateTime.now(),LocalDateTime.now(),1L);
        when(courses.rename(eq("owner"),eq(7L),any())).thenThrow(new com.cityroulette.domain.course.service.SavedCourseConflictException());
        when(courses.findOne("owner",7L)).thenReturn(response);
        mvc.perform(post("/my/courses/7/title").with(user("owner")).with(csrf())
                        .param("title","오래된 수정").param("version","0"))
                .andExpect(status().isConflict())
                .andExpect(view().name("course/detail"))
                .andExpect(content().string(containsString("다른 요청에서 코스가 수정되었습니다")))
                .andExpect(content().string(containsString("최신 제목")));
    }
}
