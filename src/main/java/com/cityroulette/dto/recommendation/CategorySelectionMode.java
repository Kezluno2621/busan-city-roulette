package com.cityroulette.dto.recommendation;

/**
 * 코스 카테고리 선택이 추천 후보에 적용되는 방식을 구분한다.
 * 기본값인 PREFERRED는 다른 유형도 허용해 코스 다양성을 유지한다.
 */
public enum CategorySelectionMode {
    PREFERRED,
    REQUIRED,
    ONLY;

    public static CategorySelectionMode defaultIfNull(CategorySelectionMode mode) {
        return mode == null ? PREFERRED : mode;
    }
}
