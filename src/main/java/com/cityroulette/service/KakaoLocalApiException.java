package com.cityroulette.service;

public class KakaoLocalApiException extends RuntimeException {

    public KakaoLocalApiException(String message) {
        super(message);
    }

    public KakaoLocalApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
