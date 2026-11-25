package com.cityroulette.domain.favorite.controller;

import com.cityroulette.domain.favorite.service.PlaceFavoriteService;
import com.cityroulette.domain.member.service.MemberService;
import com.cityroulette.domain.favorite.dto.FavoritePageResponse;
import com.cityroulette.domain.member.dto.MemberProfile;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class PlaceFavoritePageControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean PlaceFavoriteService favorites;
    @MockitoBean MemberService memberService;

    @Test void rendersMemberFavoritePage() throws Exception {
        when(memberService.getProfile(anyString())).thenReturn(new MemberProfile("owner", "회원", "owner@test.local"));
        when(favorites.list("owner", 0, 12)).thenReturn(new FavoritePageResponse(List.of(), 0, 12, 0, 0, true, true));
        mvc.perform(get("/my/favorites").with(user("owner")))
                .andExpect(status().isOk()).andExpect(view().name("favorite/list"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("저장한 즐겨찾기가 없습니다")));
    }
}
