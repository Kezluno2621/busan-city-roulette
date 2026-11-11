package com.cityroulette.dto.bus;

import java.util.Map;

public record DataVerificationSnapshot(
        long places,
        long placeSources,
        long placesWithCoordinates,
        Map<String,Long> placesByCategory,
        Map<String,Long> placesByDistrict,
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
