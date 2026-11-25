package com.cityroulette.global.util;

/** 구형 지구를 가정해 두 지점 사이의 대권 거리를 계산합니다. */
public final class DistanceCalculator {

    private static final double EARTH_RADIUS_KM = 6371.0;

    private DistanceCalculator() {
    }

    /** 유효한 위·경도를 도 단위로 받아 거리 근사값을 km로 반환합니다. */
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
        // 부동소수점 오차로 제곱근의 입력이 음수가 되는 것을 막습니다.
        double clampedHaversine = Math.max(0.0, Math.min(1.0, haversine));
        double centralAngle = 2 * Math.atan2(
                Math.sqrt(clampedHaversine), Math.sqrt(1 - clampedHaversine));

        return EARTH_RADIUS_KM * centralAngle;
    }
}
