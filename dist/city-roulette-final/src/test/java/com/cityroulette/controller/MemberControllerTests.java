package com.cityroulette.controller;

import com.cityroulette.repository.MemberRepository;
import com.cityroulette.service.MemberService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import static com.cityroulette.support.MemberTestData.PASSWORD;
import static com.cityroulette.support.MemberTestData.signup;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MemberControllerTests {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private MemberService memberService;
    @Autowired
    private MemberRepository memberRepository;

    @Test
    void validSignupPersistsMemberAndRedirectsToLogin() throws Exception {
        mockMvc.perform(validSignup())
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        assertThat(memberRepository.findByUsername("signupuser")).isPresent();
    }

    @ParameterizedTest
    @CsvSource({"username,abc", "username,user name", "password,short", "email,invalid-email",
            "displayName,한", "passwordConfirm,different"})
    void invalidInputRendersFieldErrorWithoutPersistingOrEchoingPassword(String field, String value)
            throws Exception {
        MockHttpServletRequestBuilder request = post("/signup").with(csrf());
        request.param("username", field.equals("username") ? value : "signupuser")
                .param("password", field.equals("password") ? value : PASSWORD)
                .param("passwordConfirm", field.equals("passwordConfirm") ? value : PASSWORD)
                .param("email", field.equals("email") ? value : "signup@example.com")
                .param("displayName", field.equals("displayName") ? value : "테스트유저");

        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(view().name("signup"))
                .andExpect(model().attributeHasFieldErrors("signupRequest", field))
                .andExpect(content().string(not(containsString(PASSWORD))));
        assertThat(memberRepository.count()).isZero();
    }

    @Test
    void blankInputRendersValidationErrors() throws Exception {
        mockMvc.perform(post("/signup").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("signup"))
                .andExpect(model().attributeHasFieldErrors("signupRequest", "username", "password",
                        "passwordConfirm", "email", "displayName"));
    }

    @ParameterizedTest
    @CsvSource({"signupuser,other@example.com,username,이미 사용 중인 아이디입니다.",
            "otheruser,signup@example.com,email,이미 가입된 이메일입니다."})
    void duplicateMemberRendersReadableFieldError(String username, String email, String field, String message)
            throws Exception {
        memberService.register(signup("signupuser", "signup@example.com"));

        mockMvc.perform(post("/signup").with(csrf())
                        .param("username", username).param("password", PASSWORD)
                        .param("passwordConfirm", PASSWORD).param("email", email)
                        .param("displayName", "테스트유저"))
                .andExpect(status().isOk())
                .andExpect(view().name("signup"))
                .andExpect(model().attributeHasFieldErrors("signupRequest", field))
                .andExpect(content().string(containsString(message)))
                .andExpect(content().string(not(containsString(PASSWORD))));
        assertThat(memberRepository.count()).isEqualTo(1);
    }

    @Test
    void signupAndLoginFormsIncludeCsrfTokenAndPasswordFieldsAreEmpty() throws Exception {
        for (String path : new String[]{"/signup", "/login"}) {
            mockMvc.perform(get(path))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("name=\"_csrf\"")))
                    .andExpect(content().string(containsString("type=\"password\"")));
        }
    }

    private MockHttpServletRequestBuilder validSignup() {
        return post("/signup").with(csrf())
                .param("username", "signupuser").param("password", PASSWORD)
                .param("passwordConfirm", PASSWORD).param("email", "signup@example.com")
                .param("displayName", "테스트유저");
    }
}
