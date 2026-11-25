package com.cityroulette.service;

import com.cityroulette.domain.BusRoute;
import com.cityroulette.domain.BusRouteStop;
import com.cityroulette.domain.BusStop;
import com.cityroulette.domain.Place;
import com.cityroulette.dto.bus.BusPageResponse;
import com.cityroulette.dto.bus.BusRouteResponse;
import com.cityroulette.dto.bus.BusRouteStopResponse;
import com.cityroulette.dto.bus.BusStopResponse;
import com.cityroulette.dto.bus.NearbyBusStopResponse;
import com.cityroulette.repository.BusRouteRepository;
import com.cityroulette.repository.BusRouteStopRepository;
import com.cityroulette.repository.BusStopRepository;
import com.cityroulette.repository.PlaceRepository;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 저장된 버스 정적 데이터를 조회하고 장소 주변 정류소를 계산합니다.
 *
 * <p>주변 검색은 DB bounding box로 후보를 줄인 뒤 Haversine 직선거리로 최종
 * 반경을 판정합니다. 반환 거리는 실제 도보 거리가 아닙니다.</p>
 */
@Service
@Transactional(readOnly = true)
public class BusQueryService {

    private static final double METERS_PER_LATITUDE_DEGREE = 110_000.0;
    private static final int MAX_NEARBY_RESULTS = 100;

    private final BusStopRepository stops;
    private final BusRouteRepository routes;
    private final BusRouteStopRepository routeStops;
    private final PlaceRepository places;

    public BusQueryService(
            BusStopRepository stops,
            BusRouteRepository routes,
            BusRouteStopRepository routeStops,
            PlaceRepository places) {
        this.stops = stops;
        this.routes = routes;
        this.routeStops = routeStops;
        this.places = places;
    }

    public BusPageResponse<BusStopResponse> stops(String name, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("name", "id"));
        Page<BusStop> data = name == null || name.isBlank()
                ? stops.findAll(pageable)
                : stops.findByNameContainingIgnoreCase(name.trim(), pageable);
        return page(data.map(this::stop));
    }

    public BusStopResponse stop(Long id) {
        return stop(stops.findById(id)
                .orElseThrow(() -> new BusResourceNotFoundException("정류소", id)));
    }

    public BusPageResponse<BusRouteResponse> routes(String number, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("routeNumber", "id"));
        Page<BusRoute> data = number == null || number.isBlank()
                ? routes.findAll(pageable)
                : routes.findByRouteNumberContainingIgnoreCase(number.trim(), pageable);
        return page(data.map(this::route));
    }

    public BusRouteResponse route(Long id) {
        return route(routes.findById(id)
                .orElseThrow(() -> new BusResourceNotFoundException("노선", id)));
    }

    public List<BusRouteStopResponse> routeStops(Long id) {
        if (!routes.existsById(id)) {
            throw new BusResourceNotFoundException("노선", id);
        }
        return routeStops.findByBusRouteIdOrderByStopSequence(id).stream()
                .map(link -> new BusRouteStopResponse(
                        link.getStopSequence(),
                        link.getDirection(),
                        stop(link.getBusStop())))
                .toList();
    }

    public List<BusRouteResponse> stopRoutes(Long stopId) {
        if (!stops.existsById(stopId)) {
            throw new BusResourceNotFoundException("정류소", stopId);
        }
        return routeStops.findByBusStopIdOrderByBusRouteRouteNumberAsc(stopId).stream()
                .map(BusRouteStop::getBusRoute)
                .distinct()
                .map(this::route)
                .toList();
    }

    /** 여러 정류소의 경유 노선을 한 번에 읽어 N+1 조회를 피합니다. */
    public Map<Long, List<BusRouteResponse>> routesByStopIds(Collection<Long> stopIds) {
        if (stopIds == null || stopIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, LinkedHashMap<Long, BusRouteResponse>> grouped = new LinkedHashMap<>();
        for (BusRouteStop link : routeStops.findWithRoutesByBusStopIds(stopIds)) {
            Long stopId = link.getBusStop().getId();
            BusRouteResponse response = route(link.getBusRoute());
            grouped.computeIfAbsent(stopId, key -> new LinkedHashMap<>())
                    .putIfAbsent(response.id(), response);
        }
        Map<Long, List<BusRouteResponse>> result = new LinkedHashMap<>();
        grouped.forEach((stopId, byRoute) ->
                result.put(stopId, List.copyOf(byRoute.values())));
        return result;
    }

    public NearbyBusStopResponse nearby(Long placeId, int radius) {
        Place place = places.findById(placeId)
                .orElseThrow(() -> new PlaceNotFoundException(placeId));
        if (place.getLatitude() == null || place.getLongitude() == null) {
            throw new PlaceCoordinatesMissingException(placeId);
        }

        double latitude = place.getLatitude();
        double longitude = place.getLongitude();
        BoundingBox box = boundingBox(latitude, longitude, radius);
        List<NearbyBusStopResponse.NearbyStop> result = stops.findWithinBoundingBox(
                        box.minLatitude(),
                        box.maxLatitude(),
                        box.minLongitude(),
                        box.maxLongitude()).stream()
                .map(stop -> new NearbyBusStopResponse.NearbyStop(
                        stop(stop),
                        distance(
                                latitude,
                                longitude,
                                stop.getLatitude().doubleValue(),
                                stop.getLongitude().doubleValue())))
                .filter(candidate -> candidate.distanceMeters() <= radius)
                .sorted(Comparator.comparingDouble(
                        NearbyBusStopResponse.NearbyStop::distanceMeters))
                .limit(MAX_NEARBY_RESULTS)
                .toList();
        return new NearbyBusStopResponse(
                placeId,
                radius,
                "HAVERSINE_STRAIGHT_LINE_NOT_WALKING_DISTANCE",
                result);
    }

    public boolean hasNearbyStop(double latitude, double longitude, int radius) {
        BoundingBox box = boundingBox(latitude, longitude, radius);
        return stops.findWithinBoundingBox(
                        box.minLatitude(),
                        box.maxLatitude(),
                        box.minLongitude(),
                        box.maxLongitude()).stream()
                .anyMatch(stop -> distance(
                        latitude,
                        longitude,
                        stop.getLatitude().doubleValue(),
                        stop.getLongitude().doubleValue()) <= radius);
    }

    private BoundingBox boundingBox(double latitude, double longitude, int radius) {
        double latitudeDelta = radius / METERS_PER_LATITUDE_DEGREE;
        double longitudeDelta = radius / (METERS_PER_LATITUDE_DEGREE
                * Math.max(Math.cos(Math.toRadians(latitude)), 0.000001));
        return new BoundingBox(
                BigDecimal.valueOf(latitude - latitudeDelta),
                BigDecimal.valueOf(latitude + latitudeDelta),
                BigDecimal.valueOf(longitude - longitudeDelta),
                BigDecimal.valueOf(longitude + longitudeDelta));
    }

    double distance(double lat1, double lon1, double lat2, double lon2) {
        double firstLatitude = Math.toRadians(lat1);
        double secondLatitude = Math.toRadians(lat2);
        double latitudeDelta = secondLatitude - firstLatitude;
        double longitudeDelta = Math.toRadians(lon2 - lon1);
        double haversine = Math.sin(latitudeDelta / 2) * Math.sin(latitudeDelta / 2)
                + Math.cos(firstLatitude) * Math.cos(secondLatitude)
                * Math.sin(longitudeDelta / 2) * Math.sin(longitudeDelta / 2);
        return 6_371_000 * 2
                * Math.atan2(Math.sqrt(haversine), Math.sqrt(1 - haversine));
    }

    private BusStopResponse stop(BusStop stop) {
        return new BusStopResponse(
                stop.getId(),
                stop.getExternalStopId(),
                stop.getArsNumber(),
                stop.getName(),
                stop.getLatitude(),
                stop.getLongitude(),
                stop.getStopType());
    }

    private BusRouteResponse route(BusRoute route) {
        return new BusRouteResponse(
                route.getId(),
                route.getExternalRouteId(),
                route.getRouteNumber(),
                route.getRouteType(),
                route.getStartStopName(),
                route.getEndStopName());
    }

    private <T> BusPageResponse<T> page(Page<T> page) {
        return new BusPageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }

    private record BoundingBox(
            BigDecimal minLatitude,
            BigDecimal maxLatitude,
            BigDecimal minLongitude,
            BigDecimal maxLongitude) {
    }
}
