package com.cityroulette.service;

import org.springframework.stereotype.Component;

@Component
public class PlaceCoordinateNormalizer {
    private static final double MIN_LAT = 34.80, MAX_LAT = 35.40;
    private static final double MIN_LON = 128.70, MAX_LON = 129.35;

    public Coordinates normalize(String latitude, String longitude) {
        Double lat = parse(latitude), lon = parse(longitude);
        if (!isValidBusan(lat, lon)) return new Coordinates(null, null);
        return new Coordinates(lat, lon);
    }

    public boolean isValidBusan(Double latitude, Double longitude) {
        return latitude != null && longitude != null
                && Double.isFinite(latitude) && Double.isFinite(longitude)
                && latitude >= MIN_LAT && latitude <= MAX_LAT
                && longitude >= MIN_LON && longitude <= MAX_LON;
    }

    private Double parse(String value) {
        if (value == null || value.isBlank()) return null;
        try { return Double.valueOf(value.trim()); }
        catch (NumberFormatException exception) { return null; }
    }

    public record Coordinates(Double latitude, Double longitude) { }
}
