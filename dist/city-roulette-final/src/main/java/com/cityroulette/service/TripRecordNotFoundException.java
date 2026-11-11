package com.cityroulette.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class TripRecordNotFoundException extends RuntimeException {

    public TripRecordNotFoundException() {
        super("여행 기록을 찾을 수 없습니다.");
    }
}
