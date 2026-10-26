package com.cityroulette.service;

public class RegistrationException extends IllegalArgumentException {
    private final String field;

    public RegistrationException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() { return field; }
}
