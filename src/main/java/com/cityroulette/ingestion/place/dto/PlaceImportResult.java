package com.cityroulette.ingestion.place.dto;

public record PlaceImportResult(String provider, boolean dryRun, int fetched, int inserted,
                                int updated, int duplicates, int skipped, int failed) {
    public static PlaceImportResult empty(String provider, boolean dryRun) {
        return new PlaceImportResult(provider, dryRun, 0, 0, 0, 0, 0, 0);
    }
    public PlaceImportResult add(int fetched, int inserted, int updated, int duplicates, int skipped, int failed) {
        return new PlaceImportResult(provider, dryRun, this.fetched + fetched, this.inserted + inserted,
                this.updated + updated, this.duplicates + duplicates, this.skipped + skipped, this.failed + failed);
    }
}
