package com.cityroulette.ingestion.place.dto;

public record ProviderInspection(
        String provider,
        int reportedTotalCount,
        int sampleCount,
        int mappingFailures) {
}
