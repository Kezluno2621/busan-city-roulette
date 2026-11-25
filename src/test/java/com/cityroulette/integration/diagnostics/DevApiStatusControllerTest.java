package com.cityroulette.integration.diagnostics;

import com.cityroulette.domain.member.service.MemberService;
import com.cityroulette.domain.member.dto.MemberProfile;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"test", "local"})
class DevApiStatusControllerTest {
    @Autowired MockMvc mockMvc;
    @MockitoBean ExternalApiDiagnosticsService diagnosticsService;
    @MockitoBean MemberService memberService;

    @Test
    void adminCanExplicitlyRunDiagnostics() throws Exception {
        when(memberService.getProfile(anyString()))
                .thenReturn(new MemberProfile("admin", "관리자", "admin@example.com"));
        when(diagnosticsService.probeAll()).thenReturn(List.of(
                ExternalApiProbeResult.connected("TOUR_API", 1, Set.of("contentid", "title"), "0000")));

        mockMvc.perform(get("/dev/api-status").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].provider").value("TOUR_API"))
                .andExpect(jsonPath("$[0].status").value("CONNECTED"))
                .andExpect(jsonPath("$[0].configured").value(true))
                .andExpect(jsonPath("$[0].responseFields").isArray());

        verify(diagnosticsService).probeAll();
    }

    @Test
    void guestAndOrdinaryUserCannotRunDiagnostics() throws Exception {
        mockMvc.perform(get("/dev/api-status")).andExpect(status().is3xxRedirection());
        mockMvc.perform(get("/dev/api-status").with(user("member").roles("USER")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(diagnosticsService);
    }
}
