package com.cityroulette.dto.course;

/** 화면에서 선택한 두 코스 생성 기능을 서버에서도 명확히 구분한다. */
public enum CourseGenerationMode {
    INSTANT,
    PERSONALIZED;

    public static CourseGenerationMode defaultIfNull(CourseGenerationMode mode) {
        return mode == null ? PERSONALIZED : mode;
    }
}
