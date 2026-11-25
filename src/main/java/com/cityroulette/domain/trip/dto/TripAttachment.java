package com.cityroulette.domain.trip.dto;

import org.springframework.core.io.Resource;

public record TripAttachment(
        Resource resource,
        String originalFileName,
        String contentType,
        long size) {
}
