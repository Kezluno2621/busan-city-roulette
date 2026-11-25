package com.cityroulette.ingestion.tourapi.dto;

public record TourismSyncResult(int requested, int inserted, int updated, int skipped) {
}
