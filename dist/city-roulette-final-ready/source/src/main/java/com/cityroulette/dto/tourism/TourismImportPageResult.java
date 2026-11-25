package com.cityroulette.dto.tourism;

public record TourismImportPageResult(
        int received, int inserted, int updated, int duplicates, int skipped, int failed) { }
