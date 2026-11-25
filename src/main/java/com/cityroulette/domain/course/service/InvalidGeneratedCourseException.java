package com.cityroulette.domain.course.service;

public class InvalidGeneratedCourseException extends RuntimeException {
    public InvalidGeneratedCourseException() {
        super("저장 토큰이 없거나 만료되었습니다. 코스를 다시 생성해주세요.");
    }
}
