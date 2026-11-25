package com.cityroulette.integration.diagnostics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** 명시적으로 켠 경우 외부 API 연결 상태를 점검합니다. */
@Component
@ConditionalOnProperty(name = "external.api.diagnostics.enabled", havingValue = "true")
public class ExternalApiDiagnosticsRunner implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(ExternalApiDiagnosticsRunner.class);
    private final ExternalApiDiagnosticsService service;
    public ExternalApiDiagnosticsRunner(ExternalApiDiagnosticsService service) { this.service = service; }
    @Override public void run(ApplicationArguments args) {
        for (ExternalApiProbeResult result : service.probeAll()) {
            log.info("External API diagnostic: provider={}, status={}, configured={}, httpStatus={}, providerCode={}, parsed={}, sampleCount={}, totalCount={}, fields={}, message={}",
                    result.provider(), result.status(), result.configured(), result.httpStatus(),
                    result.providerCode(), result.parsed(), result.sampleCount(), result.totalCount(),
                    result.responseFields(), result.message());
        }
    }
}
