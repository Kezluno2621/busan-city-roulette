package com.cityroulette.dto.importing;

import java.time.LocalDateTime;

public record NormalizedPlace(
        RecordType recordType,
        String provider,
        String externalId,
        String name,
        String normalizedName,
        String category,
        String address,
        String normalizedAddress,
        String district,
        Double latitude,
        Double longitude,
        String description,
        String imageUrl,
        String sourceUrl,
        String sourceCategory,
        LocalDateTime sourceModifiedAt,
        String contentLicense,
        String imageLicense,
        String attribution) {

    public enum RecordType { PLACE, THEME_CONTENT, COURSE_CONTENT }
}
