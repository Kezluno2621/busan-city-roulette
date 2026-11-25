package com.cityroulette.service;

public class OdsayApiException extends RuntimeException {

    private final String errorCode;

    public OdsayApiException(String message) {
        super(message);
        this.errorCode = null;
    }

    public OdsayApiException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = null;
    }

    public OdsayApiException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public boolean isRouteNotFound() {
        return "400".equals(errorCode);
    }
}
