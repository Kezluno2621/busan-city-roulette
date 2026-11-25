package com.cityroulette.controller;

import com.cityroulette.dto.course.CourseEstimateStatus;
import com.cityroulette.dto.member.MemberProfile;
import com.cityroulette.dto.savedcourse.*;
import com.cityroulette.service.*;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class SavedCourseControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean SavedCourseService courses;
    @MockitoBean MemberService memberService;

    @BeforeEach void memberProfiles(){when(memberService.getProfile(anyString())).thenAnswer(invocation ->
            new MemberProfile(invocation.getArgument(0),"테스트","test@example.com"));}

    @Test void guestCannotUseSavedCourseApiButCanGenerate() throws Exception {
        mvc.perform(get("/api/courses")).andExpect(status().is3xxRedirection());
        mvc.perform(post("/api/courses").with(csrf()).contentType("application/json")
                .content("{\"title\":\"코스\",\"generationToken\":\"token\"}"))
                .andExpect(status().is3xxRedirection());
    }

    @Test void authenticatedMemberSavesUsingPrincipalAndCsrf() throws Exception {
        when(courses.save(eq("owner"), any())).thenReturn(response(7L, "코스", 0L));
        mvc.perform(post("/api/courses").with(user("owner")).with(csrf()).contentType("application/json")
                        .content("{\"title\":\"코스\",\"generationToken\":\"valid-token\"}"))
                .andExpect(status().isCreated()).andExpect(header().string("Location", "/api/courses/7"))
                .andExpect(jsonPath("$.id").value(7));
        verify(courses).save(eq("owner"), any());
        mvc.perform(post("/api/courses").with(user("owner")).contentType("application/json")
                        .content("{\"title\":\"코스\",\"generationToken\":\"valid-token\"}"))
                .andExpect(status().isForbidden());
    }

    @Test void supportsOwnedListDetailRenameDeleteAndMapsHiddenResourceTo404() throws Exception {
        when(courses.list("owner",0,20,"")).thenReturn(new SavedCoursePageResponse(List.of(),0,20,0,0));
        when(courses.findOne("owner",7L)).thenReturn(response(7L,"코스",0L));
        when(courses.rename(eq("owner"),eq(7L),any())).thenReturn(response(7L,"수정",1L));
        mvc.perform(get("/api/courses").with(user("owner"))).andExpect(status().isOk());
        mvc.perform(get("/api/courses/7").with(user("owner"))).andExpect(status().isOk());
        mvc.perform(patch("/api/courses/7").with(user("owner")).with(csrf()).contentType("application/json")
                .content("{\"title\":\"수정\",\"version\":0}")).andExpect(status().isOk());
        mvc.perform(delete("/api/courses/7").with(user("owner")).with(csrf())).andExpect(status().isNoContent());
        when(courses.findOne("other",7L)).thenThrow(new SavedCourseNotFoundException());
        mvc.perform(get("/api/courses/7").with(user("other"))).andExpect(status().isNotFound());
    }

    @Test void validatesTitleAndMapsOptimisticConflict() throws Exception {
        mvc.perform(post("/api/courses").with(user("owner")).with(csrf()).contentType("application/json")
                .content("{\"title\":\"\",\"generationToken\":\"token\"}"))
                .andExpect(status().isBadRequest());
        when(courses.rename(eq("owner"),eq(7L),any())).thenThrow(new SavedCourseConflictException());
        mvc.perform(patch("/api/courses/7").with(user("owner")).with(csrf()).contentType("application/json")
                .content("{\"title\":\"수정\",\"version\":0}")).andExpect(status().isConflict());
    }

    private SavedCourseResponse response(Long id,String title,Long version){return new SavedCourseResponse(id,title,"출발",35.1,129.1,1,1,50,180,30000L,CourseEstimateStatus.UNKNOWN,CourseEstimateStatus.UNKNOWN,List.of(),List.of(),LocalDateTime.now(),LocalDateTime.now(),version);}
}
