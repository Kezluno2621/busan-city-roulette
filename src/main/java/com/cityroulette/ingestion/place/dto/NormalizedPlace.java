package com.cityroulette.ingestion.place.dto;

import java.time.LocalDateTime;

/**
 * 관광 공급자별 Mapper가 만든 표준 입력입니다. 정규화된 검색값과 원천 식별자, 출처와 이용 조건을 보존하며 장소와 테마·코스 콘텐츠를 구분합니다.
 */
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
