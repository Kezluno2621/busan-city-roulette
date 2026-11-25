package com.cityroulette.ingestion.tourapi.dto;

public record TourismImportPageResult(
        int received, int inserted, int updated, int duplicates, int skipped, int failed) { }
