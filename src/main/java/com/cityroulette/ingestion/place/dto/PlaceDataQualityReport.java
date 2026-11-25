package com.cityroulette.ingestion.place.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record PlaceDataQualityReport(
        LocalDateTime measuredAt,
        long totalPlaces,
        long missingCoordinates,
        double missingCoordinateRate,
        long missingAddresses,
        double missingAddressRate,
        long missingImages,
        double missingImageRate,
        long mergeCandidates,
        Map<String, Long> categoryDistribution,
        Map<String, Long> districtDistribution,
        List<ProviderQuality> providers) {

    public record ProviderQuality(String provider, long sourceCount, long distinctPlaceCount,
            long unknownImageLicenseCount) { }
}
