package com.cityroulette.controller;

import com.cityroulette.domain.Member;
import com.cityroulette.service.MemberService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static com.cityroulette.support.MemberTestData.PASSWORD;
import static com.cityroulette.support.MemberTestData.signup;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SecurityTests {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private MemberService memberService;

    @ParameterizedTest
    @ValueSource(strings = {"/", "/recommend", "/login", "/signup", "/css/style.css", "/js/main.js"})
    void guestCanAccessPublicPagesAndResources(String path) throws Exception {
        mockMvc.perform(get(path)).andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/places", "/api/places/test", "/api/tourism/test",
            "/api/transit/test", "/api/recommendations/test"})
    void unimplementedPublicEndpointsReturnNotFoundRatherThanAuthenticationError(String path) throws Exception {
        mockMvc.perform(get(path)).andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/my", "/my/history"})
    void guestIsRedirectedToCustomLoginForMemberPages(String path) throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", endsWith("/login")));
    }

    @Test
    void realLoginPersistsAuthenticationAndRendersProfileWithoutPassword() throws Exception {
        Member member = memberService.register(signup("loginuser", "login@example.com"));
        MockHttpSession session = login();

        mockMvc.perform(get("/my").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("my/index"))
                .andExpect(content().string(containsString("loginuser")))
                .andExpect(content().string(containsString("테스트유저")))
                .andExpect(content().string(containsString("login@example.com")))
                .andExpect(content().string(not(containsString(member.getPassword()))))
                .andExpect(content().string(not(containsString(PASSWORD))));
        mockMvc.perform(get("/").session(session))
                .andExpect(content().string(containsString("테스트유저")))
                .andExpect(content().string(containsString("/logout")));
    }

    @Test
    void failedLoginUsesGenericErrorAndDoesNotAuthenticate() throws Exception {
        memberService.register(signup("loginuser", "login@example.com"));

        mockMvc.perform(formLogin().user("loginuser").password("incorrect-value"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"))
                .andExpect(unauthenticated());
        mockMvc.perform(formLogin().user("missinguser").password(PASSWORD))
                .andExpect(redirectedUrl("/login?error"))
                .andExpect(unauthenticated());
        mockMvc.perform(get("/login?error"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("아이디 또는 비밀번호가 올바르지 않습니다.")));
    }

    @Test
    void successfulLoginReturnsToOriginallyRequestedMemberPage() throws Exception {
        memberService.register(signup("loginuser", "login@example.com"));
        MockHttpSession session = (MockHttpSession) mockMvc.perform(get("/my"))
                .andReturn().getRequest().getSession(false);
        assertThat(session).isNotNull();

        mockMvc.perform(post("/login").session(session).with(csrf())
                        .param("username", "loginuser").param("password", PASSWORD))
                .andExpect(authenticated().withUsername("loginuser"))
                .andExpect(header().string("Location", containsString("/my")));
    }

    @Test
    void logoutRequiresCsrfAndInvalidatesSession() throws Exception {
        memberService.register(signup("loginuser", "login@example.com"));
        MockHttpSession session = login();

        mockMvc.perform(post("/logout").session(session)).andExpect(status().isForbidden());
        mockMvc.perform(get("/my").session(session)).andExpect(status().isOk());
        mockMvc.perform(post("/logout").session(session).with(csrf()))
                .andExpect(redirectedUrl("/"))
                .andExpect(unauthenticated());
        assertThat(session.isInvalid()).isTrue();
        mockMvc.perform(get("/my"))
                .andExpect(header().string("Location", endsWith("/login")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/signup", "/login"})
    void stateChangingFormsRejectMissingCsrf(String path) throws Exception {
        mockMvc.perform(post(path)).andExpect(status().isForbidden());
    }

    private MockHttpSession login() throws Exception {
        return (MockHttpSession) mockMvc.perform(formLogin().user("loginuser").password(PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(authenticated().withUsername("loginuser"))
                .andReturn().getRequest().getSession(false);
    }
}
