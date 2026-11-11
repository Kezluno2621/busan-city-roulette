package com.cityroulette.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class SavedCourseConflictException extends RuntimeException {
    public SavedCourseConflictException() { super("다른 요청에서 코스가 수정되었습니다. 새로고침 후 다시 시도해주세요."); }
}
