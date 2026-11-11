package com.cityroulette.service;

import com.cityroulette.domain.Place;
import com.cityroulette.dto.bus.TransitDataCoverageStatus;
import com.cityroulette.dto.bus.TransitRoutePlanResponse;
import com.cityroulette.dto.bus.TransitVerificationStatus;
import com.cityroulette.dto.course.CourseEstimateStatus;
import com.cityroulette.dto.course.CourseGenerationRequest;
import com.cityroulette.dto.course.CourseGenerationResponse;
import com.cityroulette.dto.course.CourseOriginDto;
import com.cityroulette.dto.course.CoursePlaceDto;
import com.cityroulette.dto.recommendation.PersonalizedRecommendationResponse;
import com.cityroulette.dto.recommendation.RecommendationRequest;
import com.cityroulette.dto.recommendation.RecommendationResultDto;
import com.cityroulette.dto.recommendation.RecommendationTransitStatus;
import com.cityroulette.repository.PlaceRepository;
import com.cityroulette.util.DistanceCalculator;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 개인화 추천 결과를 가중치 기반으로 선택하고 방문 순서를 구성하는 코스 생성 서비스입니다.
 *
 * <p>같은 seed와 같은 후보 집합은 같은 결과를 만들며, 계산된 직선 연결을 실제 도로나
 * 버스 운행 경로로 표현하지 않습니다.</p>
 */
@Service
@Transactional(readOnly = true)
public class CourseGenerationService {
    private static final int RECOMMENDATION_POOL_LIMIT = 20;
    private static final int TRANSIT_RADIUS_METERS = 1_000;
    private static final double CATEGORY_NOVELTY_BONUS = 12.0;
    private static final double MAX_DIVERSITY_DISTANCE_FACTOR = 1.35;
    private static final double MAX_DIVERSITY_DISTANCE_ALLOWANCE_METERS = 1_000;

    private final PersonalizedRecommendationService recommendations;
    private final TransitRoutePlannerService transitPlanner;
    private final PlaceRepository places;
    private final GeneratedCourseRegistry generatedCourses;

    public CourseGenerationService(PersonalizedRecommendationService recommendations,
            TransitRoutePlannerService transitPlanner, PlaceRepository places,
            GeneratedCourseRegistry generatedCourses) {
        this.recommendations = recommendations;
        this.transitPlanner = transitPlanner;
        this.places = places;
        this.generatedCourses = generatedCourses;
    }

    public CourseGenerationResponse generate(CourseGenerationRequest request) {
        Origin origin = resolveOrigin(request);
        int requestedCount = request.placeCount() == null ? 3 : request.placeCount();
        int randomness = request.randomness() == null ? 0 : request.randomness();
        RecommendationRequest recommendationRequest = new RecommendationRequest(
                request.originPlaceId(), request.originLatitude(), request.originLongitude(),
                request.categories(), request.district(), request.availableMinutes(), request.budget(),
                0, RECOMMENDATION_POOL_LIMIT, request.randomSeed());
        PersonalizedRecommendationResponse recommendationResponse = recommendations.recommend(recommendationRequest);

        List<RecommendationResultDto> selected = select(
                recommendationResponse.recommendations(), requestedCount, randomness, request.randomSeed());
        List<OrderedCandidate> ordered = diversityAwareOrder(origin, selected);
        List<CoursePlaceDto> coursePlaces = buildCoursePlaces(origin, ordered);
        boolean insufficient = coursePlaces.size() < requestedCount;
        LinkedHashSet<String> limitations = new LinkedHashSet<>(recommendationResponse.dataLimitations());
        limitations.add("장소 순서는 직선거리 기반 휴리스틱이며 최적 경로나 실제 이동 경로가 아닙니다.");
        if (hasMealAdjacency(ordered)) {
            limitations.add("후보 구성과 이동거리 제한으로 음식점이 연속될 수 있습니다.");
        } else if (selected.stream().filter(item -> role(item) == PlaceRole.MEAL).count() > 1) {
            limitations.add("장소 역할 다양성을 고려해 음식점 연속 방문을 줄였습니다.");
        }
        limitations.add("방문 소요시간과 비용 데이터가 없어 두 상태는 UNKNOWN입니다.");
        if (insufficient) limitations.add("필수 검증을 통과한 후보가 요청 장소 수보다 적습니다.");

        String message = insufficient
                ? "요청한 장소 수보다 후보가 적어 가능한 장소만으로 코스를 만들었습니다."
                : "추천 점수와 카테고리 다양성을 반영해 즉흥 코스를 만들었습니다.";
        CourseGenerationResponse response = new CourseGenerationResponse("부산 즉흥 여행 코스 · " + coursePlaces.size() + "곳",
                origin.dto(), Instant.now(), requestedCount, coursePlaces.size(), coursePlaces,
                insufficient, message, List.copyOf(limitations), null);
        String saveToken = coursePlaces.isEmpty() ? null : generatedCourses.issue(request, response);
        return new CourseGenerationResponse(response.courseTitle(), response.origin(), response.generatedAt(),
                response.requestedPlaceCount(), response.actualPlaceCount(), response.places(),
                response.insufficientCandidates(), response.message(), response.dataLimitations(), saveToken);
    }

    private Origin resolveOrigin(CourseGenerationRequest request) {
        boolean hasId = request.originPlaceId() != null;
        boolean hasLatitude = request.originLatitude() != null;
        boolean hasLongitude = request.originLongitude() != null;
        if (hasId && (hasLatitude || hasLongitude)) {
            throw new IllegalArgumentException("출발 장소 ID와 출발 좌표는 동시에 입력할 수 없습니다.");
        }
        if (!hasId && hasLatitude != hasLongitude) {
            throw new IllegalArgumentException("출발 위도와 경도는 함께 입력해야 합니다.");
        }
        if (!hasId && !hasLatitude) {
            throw new IllegalArgumentException("출발 장소 ID 또는 출발 좌표가 필요합니다.");
        }
        if (hasId) {
            Place place = places.findById(request.originPlaceId())
                    .orElseThrow(() -> new PlaceNotFoundException(request.originPlaceId()));
            if (place.getLatitude() == null || place.getLongitude() == null) {
                throw new PlaceCoordinatesMissingException(place.getId());
            }
            return new Origin(place.getId(), place.getName(), place.getLatitude(), place.getLongitude());
        }
        if (!Double.isFinite(request.originLatitude()) || !Double.isFinite(request.originLongitude())
                || request.originLatitude() < -90 || request.originLatitude() > 90
                || request.originLongitude() < -180 || request.originLongitude() > 180) {
            throw new IllegalArgumentException("출발 좌표 범위를 확인해주세요.");
        }
        return new Origin(null, "사용자 지정 출발지", request.originLatitude(), request.originLongitude());
    }

    private List<RecommendationResultDto> select(List<RecommendationResultDto> candidates,
            int requestedCount, int randomness, Long requestedSeed) {
        if (candidates.isEmpty()) return List.of();
        int count = Math.min(requestedCount, candidates.size());
        LinkedHashMap<Long, RecommendationResultDto> unique = new LinkedHashMap<>();
        candidates.stream().filter(this::hasValidCoordinates)
                .forEach(candidate -> unique.putIfAbsent(candidate.placeId(), candidate));
        List<RecommendationResultDto> available = new ArrayList<>(unique.values());
        count = Math.min(requestedCount, available.size());
        List<RecommendationResultDto> selected = new ArrayList<>();
        Set<PlaceRole> selectedRoles = new LinkedHashSet<>();
        long seed = requestedSeed == null ? ThreadLocalRandom.current().nextLong() : requestedSeed;
        SplittableRandom random = new SplittableRandom(seed);

        while (selected.size() < count && !available.isEmpty()) {
            RecommendationResultDto chosen;
            if (randomness == 0) {
                chosen = available.stream().max(Comparator
                        .comparingDouble((RecommendationResultDto item) -> deterministicWeight(item, selectedRoles))
                        .thenComparingDouble(item -> -item.distanceMeters())
                        .thenComparing(item -> -item.placeId())).orElseThrow();
            } else {
                int remaining = count - selected.size();
                int poolSize = Math.min(available.size(), remaining + (int) Math.ceil(
                        (available.size() - remaining) * randomness / 100.0));
                List<RecommendationResultDto> pool = available.subList(0, poolSize);
                double exponent = 4.0 - 3.3 * randomness / 100.0;
                double total = pool.stream().mapToDouble(item -> randomWeight(item, selectedRoles, exponent)).sum();
                double pick = random.nextDouble(total);
                chosen = pool.get(pool.size() - 1);
                for (RecommendationResultDto item : pool) {
                    pick -= randomWeight(item, selectedRoles, exponent);
                    if (pick <= 0) { chosen = item; break; }
                }
            }
            selected.add(chosen);
            selectedRoles.add(role(chosen));
            available.remove(chosen);
        }
        return selected;
    }

    private double deterministicWeight(RecommendationResultDto item, Set<PlaceRole> selectedRoles) {
        return item.score() + (selectedRoles.contains(role(item)) ? 0 : CATEGORY_NOVELTY_BONUS);
    }

    private double randomWeight(RecommendationResultDto item, Set<PlaceRole> selectedRoles, double exponent) {
        double diversity = selectedRoles.contains(role(item)) ? 1.0 : 1.35;
        return Math.pow(Math.max(1, item.score() + 1), exponent) * diversity;
    }

    private String categoryKey(RecommendationResultDto item) {
        return item.category() == null ? "" : item.category().trim().toLowerCase(Locale.ROOT);
    }

    private PlaceRole role(RecommendationResultDto item) {
        return switch (categoryKey(item)) {
            case "음식점", "식사", "restaurant" -> PlaceRole.MEAL;
            case "카페", "디저트", "cafe" -> PlaceRole.CAFE;
            case "관광지", "명소", "attraction" -> PlaceRole.SIGHTSEEING;
            case "문화시설", "박물관", "전시", "culture" -> PlaceRole.CULTURE;
            case "공원", "산책", "갈맷길", "walking" -> PlaceRole.WALKING;
            default -> PlaceRole.OTHER;
        };
    }

    private boolean hasValidCoordinates(RecommendationResultDto item) {
        return item.latitude() != null && item.longitude() != null
                && Double.isFinite(item.latitude()) && Double.isFinite(item.longitude())
                && item.latitude() >= -90 && item.latitude() <= 90
                && item.longitude() >= -180 && item.longitude() <= 180;
    }

    private List<OrderedCandidate> nearestNeighbor(Origin origin, List<RecommendationResultDto> selected) {
        List<RecommendationResultDto> remaining = new ArrayList<>(selected);
        List<OrderedCandidate> ordered = new ArrayList<>();
        double latitude = origin.latitude();
        double longitude = origin.longitude();
        while (!remaining.isEmpty()) {
            final double currentLatitude = latitude;
            final double currentLongitude = longitude;
            RecommendationResultDto next = remaining.stream().min(Comparator
                    .comparingDouble((RecommendationResultDto item) -> distance(
                            currentLatitude, currentLongitude, item.latitude(), item.longitude()))
                    .thenComparing(RecommendationResultDto::score, Comparator.reverseOrder())
                    .thenComparing(RecommendationResultDto::placeId)).orElseThrow();
            double legDistance = distance(latitude, longitude, next.latitude(), next.longitude());
            ordered.add(new OrderedCandidate(next, legDistance));
            latitude = next.latitude();
            longitude = next.longitude();
            remaining.remove(next);
        }
        return ordered;
    }

    private List<OrderedCandidate> diversityAwareOrder(Origin origin, List<RecommendationResultDto> selected) {
        if (selected.size() < 2) return nearestNeighbor(origin, selected);
        List<OrderedCandidate> baseline = nearestNeighbor(origin, selected);
        double baselineDistance = totalDistance(baseline);
        double maximumDistance = baselineDistance * MAX_DIVERSITY_DISTANCE_FACTOR
                + MAX_DIVERSITY_DISTANCE_ALLOWANCE_METERS;
        List<List<RecommendationResultDto>> permutations = new ArrayList<>();
        permute(new ArrayList<>(), new ArrayList<>(selected), permutations);
        return permutations.stream().map(order -> ordered(origin, order))
                .filter(order -> totalDistance(order) <= maximumDistance)
                .min(Comparator.comparingInt(this::mealAdjacencies)
                        .thenComparingInt(this::sameRoleAdjacencies)
                        .thenComparingDouble(this::totalDistance)
                        .thenComparing(this::orderKey))
                .orElse(baseline);
    }

    private void permute(List<RecommendationResultDto> prefix, List<RecommendationResultDto> remaining,
            List<List<RecommendationResultDto>> result) {
        if (remaining.isEmpty()) { result.add(List.copyOf(prefix)); return; }
        for (int i = 0; i < remaining.size(); i++) {
            RecommendationResultDto candidate = remaining.remove(i);
            prefix.add(candidate);
            permute(prefix, remaining, result);
            prefix.remove(prefix.size() - 1);
            remaining.add(i, candidate);
        }
    }

    private List<OrderedCandidate> ordered(Origin origin, List<RecommendationResultDto> items) {
        List<OrderedCandidate> result = new ArrayList<>();
        double latitude = origin.latitude();
        double longitude = origin.longitude();
        for (RecommendationResultDto item : items) {
            double leg = distance(latitude, longitude, item.latitude(), item.longitude());
            result.add(new OrderedCandidate(item, leg));
            latitude = item.latitude();
            longitude = item.longitude();
        }
        return result;
    }

    private int mealAdjacencies(List<OrderedCandidate> order) {
        int count = 0;
        for (int i = 1; i < order.size(); i++) {
            if (role(order.get(i - 1).item()) == PlaceRole.MEAL
                    && role(order.get(i).item()) == PlaceRole.MEAL) count++;
        }
        return count;
    }

    private int sameRoleAdjacencies(List<OrderedCandidate> order) {
        int count = 0;
        for (int i = 1; i < order.size(); i++) {
            if (role(order.get(i - 1).item()) == role(order.get(i).item())) count++;
        }
        return count;
    }

    private boolean hasMealAdjacency(List<OrderedCandidate> order) { return mealAdjacencies(order) > 0; }
    private double totalDistance(List<OrderedCandidate> order) {
        return order.stream().mapToDouble(OrderedCandidate::distanceMeters).sum();
    }
    private String orderKey(List<OrderedCandidate> order) {
        return order.stream().map(item -> String.format("%020d", item.item().placeId()))
                .reduce("", String::concat);
    }

    private List<CoursePlaceDto> buildCoursePlaces(Origin origin, List<OrderedCandidate> ordered) {
        List<CoursePlaceDto> result = new ArrayList<>();
        Long previousPlaceId = origin.placeId();
        for (int index = 0; index < ordered.size(); index++) {
            OrderedCandidate orderedCandidate = ordered.get(index);
            RecommendationResultDto item = orderedCandidate.item();
            RecommendationTransitStatus transit = index == 0 && previousPlaceId != null
                    && item.transitVerificationStatus() != RecommendationTransitStatus.NOT_EVALUATED
                    ? item.transitVerificationStatus()
                    : evaluateTransit(previousPlaceId, item.placeId());
            LinkedHashSet<String> limitations = new LinkedHashSet<>(item.dataLimitations());
            limitations.add("이전 지점과의 거리는 Haversine 직선거리입니다.");
            if (transit != RecommendationTransitStatus.VERIFIED) {
                limitations.add("부분 정적 교통 데이터로 실제 이동 가능 여부를 단정하지 않습니다.");
            }
            result.add(new CoursePlaceDto(item.placeId(), item.placeName(), item.category(), item.address(),
                    item.latitude(), item.longitude(), index + 1, item.score(), item.recommendationReasons(),
                    round(orderedCandidate.distanceMeters()), transit, CourseEstimateStatus.UNKNOWN,
                    CourseEstimateStatus.UNKNOWN, List.copyOf(limitations), item.imageUrl()));
            previousPlaceId = item.placeId();
        }
        return result;
    }

    private RecommendationTransitStatus evaluateTransit(Long originPlaceId, Long destinationPlaceId) {
        if (originPlaceId == null) return RecommendationTransitStatus.UNVERIFIED;
        try {
            TransitRoutePlanResponse plan = transitPlanner.plan(
                    originPlaceId, destinationPlaceId, TRANSIT_RADIUS_METERS, 1);
            if (plan.verificationStatus() == TransitVerificationStatus.VERIFIED
                    && !plan.candidates().isEmpty()) return RecommendationTransitStatus.VERIFIED;
            if (plan.dataCoverageStatus() == TransitDataCoverageStatus.PARTIAL_DATA
                    || plan.verificationStatus() == TransitVerificationStatus.PARTIAL_DATA
                    || plan.verificationStatus() == TransitVerificationStatus.NO_CANDIDATE) {
                return RecommendationTransitStatus.PARTIAL_DATA;
            }
            return RecommendationTransitStatus.UNVERIFIED;
        } catch (RuntimeException exception) {
            return RecommendationTransitStatus.UNVERIFIED;
        }
    }

    private double distance(double fromLatitude, double fromLongitude, Double toLatitude, Double toLongitude) {
        if (toLatitude == null || toLongitude == null) return Double.MAX_VALUE;
        return DistanceCalculator.calculateKm(fromLatitude, fromLongitude, toLatitude, toLongitude) * 1_000;
    }

    private double round(double value) { return Math.round(value * 10.0) / 10.0; }
    private record OrderedCandidate(RecommendationResultDto item, double distanceMeters) { }
    private enum PlaceRole { MEAL, CAFE, SIGHTSEEING, CULTURE, WALKING, OTHER }
    private record Origin(Long placeId, String name, double latitude, double longitude) {
        CourseOriginDto dto() { return new CourseOriginDto(placeId, name, latitude, longitude); }
    }
}
