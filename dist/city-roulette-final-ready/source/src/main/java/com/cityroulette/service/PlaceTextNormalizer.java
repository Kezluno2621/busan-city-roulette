package com.cityroulette.service;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Locale;

@Component
public class PlaceTextNormalizer {
    public String trimToNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim().replaceAll("\\s+", " ");
    }

    public String normalizeName(String value) {
        return comparable(value);
    }

    public String normalizeAddress(String value) {
        String normalized = comparable(value);
        if (normalized == null) return null;
        return normalized.replace("부산광역시", "부산");
    }

    public String combine(String first, String second) {
        first = trimToNull(first); second = trimToNull(second);
        if (first == null) return second;
        if (second == null) return first;
        return first + " " + second;
    }

    private String comparable(String value) {
        String trimmed = trimToNull(value);
        if (trimmed == null) return null;
        return Normalizer.normalize(trimmed, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT).replaceAll("[^0-9a-z가-힣]", "");
    }
}
