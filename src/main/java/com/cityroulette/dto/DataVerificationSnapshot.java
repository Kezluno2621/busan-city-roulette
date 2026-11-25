package com.cityroulette.dto;

import java.util.Map;

/**
 * 장소와 버스 데이터를 함께 점검할 때 사용하는 읽기 전용 통계 스냅샷이다.
 */
public record DataVerificationSnapshot(
        long places,
        long placeSources,
        long placesWithCoordinates,
        Map<String, Long> placesByCategory,
        Map<String, Long> placesByDistrict,
        long busStops,
        long busStopsWithValidCoordinates,
        long busRoutes,
        long busRouteStops,
        long routesWithStops,
        long routesWithoutStops,
        double matchedWithin300Meters,
        double matchedWithin500Meters,
        double matchedWithin1000Meters) {
}
