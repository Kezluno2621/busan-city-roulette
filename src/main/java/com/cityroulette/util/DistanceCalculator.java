package com.cityroulette.util;

public final class DistanceCalculator {

    private static final double EARTH_RADIUS_KM = 6371.0;

    private DistanceCalculator() {
    }

    public static double calculateKm(
            double latitude1,
            double longitude1,
            double latitude2,
            double longitude2) {
        double latitudeDistance = Math.toRadians(latitude2 - latitude1);
        double longitudeDistance = Math.toRadians(longitude2 - longitude1);
        double startLatitude = Math.toRadians(latitude1);
        double endLatitude = Math.toRadians(latitude2);

        double haversine = Math.pow(Math.sin(latitudeDistance / 2), 2)
                + Math.cos(startLatitude) * Math.cos(endLatitude)
                * Math.pow(Math.sin(longitudeDistance / 2), 2);
        double clampedHaversine = Math.max(0.0, Math.min(1.0, haversine));
        double centralAngle = 2 * Math.atan2(
                Math.sqrt(clampedHaversine), Math.sqrt(1 - clampedHaversine));

        return EARTH_RADIUS_KM * centralAngle;
    }
}
