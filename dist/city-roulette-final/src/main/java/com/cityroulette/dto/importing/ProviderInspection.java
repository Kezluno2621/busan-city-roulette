package com.cityroulette.dto.importing;

public record ProviderInspection(
        String provider,
        int reportedTotalCount,
        int sampleCount,
        int mappingFailures) {
}
