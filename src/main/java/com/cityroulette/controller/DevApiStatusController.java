package com.cityroulette.controller;

import com.cityroulette.dto.ExternalApiProbeResult;
import com.cityroulette.service.ExternalApiDiagnosticsService;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 개발 환경에서 외부 API의 연결 상태를 확인합니다. */
@RestController
@RequestMapping("/dev")
@Profile({"local", "dev"})
public class DevApiStatusController {
    private final ExternalApiDiagnosticsService diagnosticsService;

    public DevApiStatusController(ExternalApiDiagnosticsService diagnosticsService) {
        this.diagnosticsService = diagnosticsService;
    }

    @GetMapping("/api-status")
    public List<ExternalApiProbeResult> apiStatus() {
        return diagnosticsService.probeAll();
    }
}
