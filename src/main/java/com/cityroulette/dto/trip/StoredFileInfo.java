package com.cityroulette.dto.trip;

public record StoredFileInfo(
        String originalFileName,
        String storedFileName,
        String contentType,
        long size) {
}
