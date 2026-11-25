package com.cityroulette.integration;

public class ExternalApiException extends RuntimeException {
    private final String providerCode;
    public ExternalApiException(String message) { this(message, null); }
    public ExternalApiException(String message, String providerCode) {
        super(message); this.providerCode = providerCode;
    }
    public String getProviderCode() { return providerCode; }
}
