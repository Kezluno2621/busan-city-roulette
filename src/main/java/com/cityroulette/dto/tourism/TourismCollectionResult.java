package com.cityroulette.dto.tourism;

public record TourismCollectionResult(
        long runId, String status, boolean dryRun, int pagesRequested, int received,
        int inserted, int updated, int duplicates, int skipped, int failed) { }
