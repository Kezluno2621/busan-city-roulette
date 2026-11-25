package com.cityroulette.ingestion.bus.dto;

import java.util.List;

public record BusStopEnrichmentReport(
        boolean dryRun,
        int targets,
        int matchedByExternalId,
        int matchedByArsNumber,
        int enriched,
        int unresolved,
        int reviewCandidates,
        int failed,
        List<BusStopEnrichmentIssue> issues) {
}
