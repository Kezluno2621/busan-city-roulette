package com.cityroulette.domain.favorite.controller;

import com.cityroulette.domain.favorite.dto.FavoriteAddResult;
import com.cityroulette.domain.favorite.dto.FavoritePageResponse;
import com.cityroulette.domain.favorite.dto.FavoriteResponse;
import com.cityroulette.domain.favorite.service.PlaceFavoriteService;
import com.cityroulette.domain.member.service.MemberService;
import com.cityroulette.domain.place.service.PlaceNotFoundException;
import com.cityroulette.domain.member.dto.MemberProfile;
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
class PlaceFavoriteControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean PlaceFavoriteService favorites;
    @MockitoBean MemberService memberService;

    @BeforeEach void profile() {
        when(memberService.getProfile(anyString())).thenReturn(new MemberProfile("owner", "회원", "owner@test.local"));
    }

    @Test void guestCannotUseFavoriteApiOrPage() throws Exception {
        mvc.perform(get("/api/favorites")).andExpect(status().is3xxRedirection());
        mvc.perform(post("/api/favorites").with(csrf()).contentType("application/json")
                .content("{\"placeId\":1}")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/my/favorites")).andExpect(status().is3xxRedirection());
    }

    @Test void addUsesPrincipalRequiresCsrfAndReturnsStableDuplicateResponse() throws Exception {
        var response = favorite(4L, 1L);
        when(favorites.add("owner", 1L)).thenReturn(new FavoriteAddResult(response, true), new FavoriteAddResult(response, false));
        mvc.perform(post("/api/favorites").with(user("owner")).with(csrf()).contentType("application/json")
                        .content("{\"placeId\":1}"))
                .andExpect(status().isCreated()).andExpect(header().string("Location", "/api/favorites/1"));
        mvc.perform(post("/api/favorites").with(user("owner")).with(csrf()).contentType("application/json")
                        .content("{\"placeId\":1}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.placeId").value(1));
        mvc.perform(post("/api/favorites").with(user("owner")).contentType("application/json")
                        .content("{\"placeId\":1}"))
                .andExpect(status().isForbidden());
        verify(favorites, times(2)).add("owner", 1L);
    }

    @Test void listsWithPaginationAndDeletesOnlyThroughPrincipal() throws Exception {
        when(favorites.list("owner", 1, 5)).thenReturn(new FavoritePageResponse(List.of(favorite(4L, 1L)), 1, 5, 6, 2, false, true));
        mvc.perform(get("/api/favorites").with(user("owner")).param("page", "1").param("size", "5"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].placeName").value("광안리"));
        mvc.perform(delete("/api/favorites/1").with(user("owner")).with(csrf()))
                .andExpect(status().isNoContent());
        verify(favorites).remove("owner", 1L);
    }

    @Test void missingPlaceIs404AndInvalidInputIs400() throws Exception {
        when(favorites.add("owner", 999L)).thenThrow(new PlaceNotFoundException(999L));
        mvc.perform(post("/api/favorites").with(user("owner")).with(csrf()).contentType("application/json")
                .content("{\"placeId\":999}")).andExpect(status().isNotFound());
        mvc.perform(post("/api/favorites").with(user("owner")).with(csrf()).contentType("application/json")
                .content("{}")) .andExpect(status().isBadRequest());
        mvc.perform(get("/api/favorites").with(user("owner")).param("size", "51"))
                .andExpect(status().isBadRequest());
    }

    private FavoriteResponse favorite(Long id, Long placeId) {
        return new FavoriteResponse(id, placeId, "광안리", "관광지", "부산", "수영구",
                35.1, 129.1, null, LocalDateTime.now());
    }
}
