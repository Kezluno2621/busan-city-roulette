package com.cityroulette.domain.trip.controller;

import com.cityroulette.domain.member.entity.Member;
import com.cityroulette.domain.trip.entity.TripRecord;
import com.cityroulette.domain.member.dto.MemberProfile;
import com.cityroulette.domain.trip.dto.TripAttachment;
import com.cityroulette.domain.trip.service.FileValidationException;
import com.cityroulette.domain.member.service.MemberService;
import com.cityroulette.domain.trip.service.TripRecordNotFoundException;
import com.cityroulette.domain.trip.service.TripRecordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TripRecordControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TripRecordService tripRecordService;

    @MockitoBean
    private MemberService memberService;

    @BeforeEach
    void setUp() {
        when(memberService.getProfile("tripuser"))
                .thenReturn(new MemberProfile("tripuser", "여행자", "trip@example.com"));
    }

    @Test
    void guestIsRedirectedToLogin() throws Exception {
        mockMvc.perform(get("/my/trips"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("/login")));
        mockMvc.perform(get("/my/trips/10/attachment"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("/login")));
    }

    @Test
    void memberCanOpenListAndNewForm() throws Exception {
        when(tripRecordService.findAll("tripuser")).thenReturn(List.of());

        mockMvc.perform(get("/my/trips").with(user("tripuser")))
                .andExpect(status().isOk())
                .andExpect(view().name("trip/list"))
                .andExpect(content().string(containsString("아직 저장한 여행이 없습니다.")));
        mockMvc.perform(get("/my/trips/new").with(user("tripuser"))
                        .param("placeName", "감천문화마을")
                        .param("placeAddress", "부산광역시 사하구"))
                .andExpect(status().isOk())
                .andExpect(view().name("trip/form"))
                .andExpect(model().attributeExists("tripRecordRequest"))
                .andExpect(content().string(containsString("감천문화마을")))
                .andExpect(content().string(containsString("multipart/form-data")));
    }

    @Test
    void validMultipartPostCreatesRecordAndRedirects() throws Exception {
        TripRecord saved = record(10L, false);
        when(tripRecordService.create(eq("tripuser"), any())).thenReturn(saved);
        MockMultipartFile image = new MockMultipartFile(
                "image", "busan.jpg", "image/jpeg", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/my/trips").file(image)
                        .param("title", "부산 여행")
                        .param("placeName", "감천문화마을")
                        .param("placeAddress", "부산광역시 사하구")
                        .param("memo", "즐거운 여행")
                        .param("visitDate", "2026-10-26")
                        .with(user("tripuser")).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/my/trips/10"));

        verify(tripRecordService).create(eq("tripuser"), any());
    }

    @Test
    void invalidPostRendersFormWithoutCallingService() throws Exception {
        mockMvc.perform(multipart("/my/trips")
                        .param("title", "")
                        .param("placeName", "")
                        .param("visitDate", "")
                        .with(user("tripuser")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("trip/form"))
                .andExpect(model().attributeHasFieldErrors(
                        "tripRecordRequest", "title", "placeName", "visitDate"));

        verify(tripRecordService, never()).create(any(), any());
    }

    @Test
    void invalidImageRendersFieldError() throws Exception {
        when(tripRecordService.create(eq("tripuser"), any()))
                .thenThrow(new FileValidationException("JPG, PNG, WEBP 이미지 파일만 업로드할 수 있습니다."));
        MockMultipartFile image = new MockMultipartFile(
                "image", "script.js", "application/javascript", new byte[]{1});

        mockMvc.perform(multipart("/my/trips").file(image)
                        .param("title", "부산 여행")
                        .param("placeName", "부산역")
                        .param("visitDate", "2026-10-26")
                        .with(user("tripuser")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("trip/form"))
                .andExpect(model().attributeHasFieldErrors("tripRecordRequest", "image"))
                .andExpect(content().string(containsString("JPG, PNG, WEBP")));
    }

    @Test
    void memberCanDownloadOwnedAttachment() throws Exception {
        ByteArrayResource resource = new ByteArrayResource(new byte[]{1, 2, 3});
        when(tripRecordService.loadAttachment("tripuser", 10L))
                .thenReturn(new TripAttachment(resource, "부산 사진.jpg", "image/jpeg", 3));

        mockMvc.perform(get("/my/trips/10/attachment").with(user("tripuser")))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/jpeg"))
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andExpect(header().string("Content-Length", "3"))
                .andExpect(content().bytes(new byte[]{1, 2, 3}));
    }

    @Test
    void anotherMembersRecordIsNotExposed() throws Exception {
        when(tripRecordService.findOne("tripuser", 99L))
                .thenThrow(new TripRecordNotFoundException());

        mockMvc.perform(get("/my/trips/99").with(user("tripuser")))
                .andExpect(status().isNotFound());
    }

    @Test
    void anotherMembersAttachmentIsNotExposed() throws Exception {
        when(tripRecordService.loadAttachment("tripuser", 99L))
                .thenThrow(new TripRecordNotFoundException());

        mockMvc.perform(get("/my/trips/99/attachment").with(user("tripuser")))
                .andExpect(status().isNotFound());
    }

    private TripRecord record(Long id, boolean withFile) {
        Member member = new Member("tripuser", "encoded", "trip@example.com", "여행자");
        TripRecord record = new TripRecord(member, "부산 여행", "감천문화마을", "부산광역시",
                "메모", LocalDate.of(2026, 10, 26),
                withFile ? "busan.jpg" : null,
                withFile ? "8c51da48-a45d-4fb2-a465-2a7a44946e5d.jpg" : null,
                withFile ? "image/jpeg" : null,
                withFile ? 3L : null);
        ReflectionTestUtils.setField(record, "id", id);
        return record;
    }
}
