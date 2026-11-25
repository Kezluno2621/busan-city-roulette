package com.cityroulette.domain.course.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class SavedCourseNotFoundException extends RuntimeException {
    public SavedCourseNotFoundException() { super("저장된 코스를 찾을 수 없습니다."); }
}
