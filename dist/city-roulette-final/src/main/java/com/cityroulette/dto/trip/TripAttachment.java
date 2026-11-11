package com.cityroulette.dto.trip;

import org.springframework.core.io.Resource;

public record TripAttachment(
        Resource resource,
        String originalFileName,
        String contentType,
        long size) {
}
