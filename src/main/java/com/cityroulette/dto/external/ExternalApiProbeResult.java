package com.cityroulette.dto.external;

import java.util.Set;

public record ExternalApiProbeResult(
        String provider, Status status, boolean configured, Integer httpStatus,
        String providerCode, boolean parsed, int sampleCount, int totalCount,
        Set<String> responseFields,
        String message) {
    public enum Status {
        CONNECTED, NOT_CONFIGURED, AUTH_FAILED, HTTP_ERROR, PARSE_ERROR, UNAVAILABLE
    }
    public static ExternalApiProbeResult connected(String provider, int count) {
        return connected(provider, count, Set.of(), "SUCCESS");
    }
    public static ExternalApiProbeResult connected(
            String provider, int count, Set<String> fields, String providerCode) {
        return new ExternalApiProbeResult(provider, Status.CONNECTED, true, 200,
                providerCode, true, fields.isEmpty() ? 0 : 1, count, Set.copyOf(fields), "OK");
    }
    public static ExternalApiProbeResult notConfigured(String provider) {
        return new ExternalApiProbeResult(provider, Status.NOT_CONFIGURED, false, null,
                null, false, 0, 0, Set.of(), "환경변수가 설정되지 않았습니다.");
    }
    public static ExternalApiProbeResult unavailable(String provider, String message) {
        return failure(provider, Status.UNAVAILABLE, null, null, false, message);
    }
    public static ExternalApiProbeResult failure(String provider, Status status,
            Integer httpStatus, String providerCode, boolean parsed, String message) {
        return new ExternalApiProbeResult(provider, status, true, httpStatus,
                providerCode, parsed, 0, 0, Set.of(), message);
    }
}
