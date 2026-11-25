package com.cityroulette.domain.trip.dto;

public record StoredFileInfo(
        String originalFileName,
        String storedFileName,
        String contentType,
        long size) {
}
