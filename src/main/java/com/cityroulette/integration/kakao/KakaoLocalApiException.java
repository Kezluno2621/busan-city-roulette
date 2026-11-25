package com.cityroulette.integration.kakao;

public class KakaoLocalApiException extends RuntimeException {

    private final Integer httpStatusCode;

    public KakaoLocalApiException(String message) {
        super(message);
        this.httpStatusCode = null;
    }

    public KakaoLocalApiException(String message, Throwable cause) {
        super(message, cause);
        this.httpStatusCode = null;
    }

    public KakaoLocalApiException(String message, Throwable cause, int httpStatusCode) {
        super(message, cause);
        this.httpStatusCode = httpStatusCode;
    }

    /** API 키 값이나 응답 본문을 노출하지 않고 실패 종류만 구분할 때 사용한다. */
    public Integer getHttpStatusCode() {
        return httpStatusCode;
    }
}
