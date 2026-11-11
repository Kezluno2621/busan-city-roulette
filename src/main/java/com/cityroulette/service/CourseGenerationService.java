package com.cityroulette.service;

import com.cityroulette.config.RecommendationWeights;
import com.cityroulette.domain.Place;
import com.cityroulette.dto.bus.TransitDataCoverageStatus;
import com.cityroulette.dto.bus.TransitRoutePlanResponse;
import com.cityroulette.dto.bus.TransitVerificationStatus;
import com.cityroulette.dto.course.CourseEstimateStatus;
import com.cityroulette.dto.course.CourseGenerationMode;
import com.cityroulette.dto.course.CourseGenerationRequest;
import com.cityroulette.dto.course.CourseGenerationResponse;
import com.cityroulette.dto.course.CourseOriginDto;
import com.cityroulette.dto.course.CoursePlaceDto;
import com.cityroulette.dto.recommendation.PersonalizedRecommendationResponse;
import com.cityroulette.dto.recommendation.CategorySelectionMode;
import com.cityroulette.dto.recommendation.RecommendationRequest;
import com.cityroulette.dto.recommendation.RecommendationResultDto;
import com.cityroulette.dto.recommendation.RecommendationScoreBreakdown;
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
    private static final int RECOMMENDATION_POOL_LIMIT = 200;
    private static final int TRANSIT_RADIUS_METERS = 1_000;
    private static final double DEFAULT_ORIGIN_LATITUDE = 35.1796;
    private static final double DEFAULT_ORIGIN_LONGITUDE = 129.0756;
    private static final List<String> DIVERSITY_CATEGORIES = List.of(
            "ATTRACTION", "CAFE", "RESTAURANT", "CULTURE");
    private static final double CATEGORY_NOVELTY_BONUS = 12.0;
    private static final double MAX_DIVERSITY_DISTANCE_FACTOR = 1.35;
    private static final double MAX_DIVERSITY_DISTANCE_ALLOWANCE_METERS = 1_000;

    private final PersonalizedRecommendationService recommendations;
    private final TransitRoutePlannerService transitPlanner;
    private final PlaceRepository places;
    private final GeneratedCourseRegistry generatedCourses;
    private final RecommendationWeights weights;

    public CourseGenerationService(PersonalizedRecommendationService recommendations,
            TransitRoutePlannerService transitPlanner, PlaceRepository places,
            GeneratedCourseRegistry generatedCourses, RecommendationWeights weights) {
        this.recommendations = recommendations;
        this.transitPlanner = transitPlanner;
        this.places = places;
        this.generatedCourses = generatedCourses;
        this.weights = weights;
    }

    public CourseGenerationResponse generate(CourseGenerationRequest request) {
        CourseGenerationMode generationMode = CourseGenerationMode.defaultIfNull(request.generationMode());
        CategorySelectionMode categoryMode = CategorySelectionMode.defaultIfNull(request.categoryMode());
        Origin origin = resolveOrigin(request, generationMode);
        int requestedCount = request.placeCount() == null ? 3 : request.placeCount();
        int randomness = request.randomness() == null
                ? (generationMode == CourseGenerationMode.INSTANT ? 100 : 40)
                : request.randomness();
        Double recommendationLatitude = request.originPlaceId() == null ? origin.latitude() : null;
        Double recommendationLongitude = request.originPlaceId() == null ? origin.longitude() : null;
        RecommendationRequest recommendationRequest = new RecommendationRequest(
                request.originPlaceId(), recommendationLatitude, recommendationLongitude,
                request.categories(), request.district(), request.availableMinutes(), request.budget(),
                0, RECOMMENDATION_POOL_LIMIT, request.randomSeed(), categoryMode);
        PersonalizedRecommendationResponse recommendationResponse = recommendations.recommend(recommendationRequest);

        List<RecommendationResultDto> candidates = recommendationResponse.recommendations();
        if (categoryMode == CategorySelectionMode.PREFERRED
                && request.categories() != null && !request.categories().isEmpty()) {
            // 선호 점수 상위권이 한 카테고리로 가득 차더라도 혼합 코스를 만들 수 있도록
            // 같은 DB 상한 안에서 중립 후보를 한 번 더 받아 원천 ID 기준으로 합친다.
            RecommendationRequest neutralRequest = new RecommendationRequest(
                    request.originPlaceId(), recommendationLatitude, recommendationLongitude,
                    List.of(), request.district(), request.availableMinutes(), request.budget(),
                    0, RECOMMENDATION_POOL_LIMIT, request.randomSeed(), CategorySelectionMode.PREFERRED);
            candidates = mergeCandidates(candidates, recommendations.recommend(neutralRequest).recommendations());
        }
        candidates = ensureCategoryCoverage(candidates, request, recommendationLatitude,
                recommendationLongitude, categoryMode);
        candidates = applyRecentExclusions(candidates, request, requestedCount);
        List<RecommendationResultDto> selected = select(
                candidates, requestedCount, randomness, request.randomSeed(), categoryMode, request.categories());
        List<OrderedCandidate> ordered = diversityAwareOrder(origin, selected);
        List<CoursePlaceDto> coursePlaces = buildCoursePlaces(origin, ordered);
        boolean insufficient = coursePlaces.size() < requestedCount;
        LinkedHashSet<String> limitations = new LinkedHashSet<>(recommendationResponse.dataLimitations());
        if (categoryMode == CategorySelectionMode.PREFERRED) {
            List<String> missingTypes = missingDiversityTypes(candidates);
            if (!missingTypes.isEmpty()) {
                limitations.add("현재 조건에서 " + String.join("·", missingTypes)
                        + " 후보가 없어 확인 가능한 장소 유형으로 구성했습니다.");
            }
        }
        limitations.add("장소 순서는 직선거리 기반 휴리스틱이며 최적 경로나 실제 이동 경로가 아닙니다.");
        if (hasMealAdjacency(ordered)) {
            limitations.add("후보 구성과 이동거리 제한으로 음식점이 연속될 수 있습니다.");
        } else if (selected.stream().filter(item -> role(item) == PlaceRole.MEAL).count() > 1) {
            limitations.add("장소 역할 다양성을 고려해 음식점 연속 방문을 줄였습니다.");
        }
        limitations.add("방문 소요시간과 비용 데이터가 없어 두 상태는 UNKNOWN입니다.");
        if (insufficient) limitations.add("필수 검증을 통과한 후보가 요청 장소 수보다 적습니다.");

        String successMessage = generationMode == CourseGenerationMode.INSTANT
                ? "추천 점수와 카테고리 다양성을 반영해 즉흥 코스를 만들었습니다."
                : "선호 조건과 카테고리 다양성을 반영해 맞춤 코스를 만들었습니다.";
        String message = insufficient
                ? "요청한 장소 수보다 후보가 적어 가능한 장소만으로 코스를 만들었습니다."
                : successMessage;
        String titlePrefix = generationMode == CourseGenerationMode.INSTANT ? "부산 즉흥 룰렛" : "부산 맞춤 여행 코스";
        CourseGenerationResponse response = new CourseGenerationResponse(titlePrefix + " · " + coursePlaces.size() + "곳",
                origin.dto(), Instant.now(), requestedCount, coursePlaces.size(), coursePlaces,
                insufficient, message, List.copyOf(limitations), null);
        String saveToken = coursePlaces.isEmpty() ? null : generatedCourses.issue(request, response);
        return new CourseGenerationResponse(response.courseTitle(), response.origin(), response.generatedAt(),
                response.requestedPlaceCount(), response.actualPlaceCount(), response.places(),
                response.insufficientCandidates(), response.message(), response.dataLimitations(), saveToken);
    }

    private Origin resolveOrigin(CourseGenerationRequest request, CourseGenerationMode generationMode) {
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
            if (generationMode == CourseGenerationMode.INSTANT) {
                return new Origin(null, "부산 중심 출발", DEFAULT_ORIGIN_LATITUDE, DEFAULT_ORIGIN_LONGITUDE);
            }
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

    private List<RecommendationResultDto> applyRecentExclusions(List<RecommendationResultDto> candidates,
            CourseGenerationRequest request, int requestedCount) {
        if (request.randomSeed() != null || request.excludedPlaceIds() == null
                || request.excludedPlaceIds().isEmpty()) {
            return candidates;
        }
        Set<Long> recent = new LinkedHashSet<>(request.excludedPlaceIds());
        List<RecommendationResultDto> fresh = candidates.stream()
                .filter(candidate -> !recent.contains(candidate.placeId()))
                .toList();
        // 새 후보가 충분할 때만 최근 결과를 제외한다. 후보 부족 때문에 결과 수가 줄어들지는 않게 한다.
        return fresh.size() >= requestedCount ? fresh : candidates;
    }

    private List<RecommendationResultDto> mergeCandidates(List<RecommendationResultDto> preferred,
            List<RecommendationResultDto> neutral) {
        LinkedHashMap<Long, RecommendationResultDto> merged = new LinkedHashMap<>();
        preferred.forEach(candidate -> merged.putIfAbsent(candidate.placeId(), candidate));
        neutral.forEach(candidate -> merged.putIfAbsent(candidate.placeId(), candidate));
        return new ArrayList<>(merged.values());
    }

    private List<RecommendationResultDto> ensureCategoryCoverage(List<RecommendationResultDto> candidates,
            CourseGenerationRequest request, Double originLatitude, Double originLongitude,
            CategorySelectionMode categoryMode) {
        if (categoryMode == CategorySelectionMode.ONLY) return candidates;
        List<String> targets = categoryMode == CategorySelectionMode.REQUIRED
                ? (request.categories() == null ? List.of() : request.categories())
                : DIVERSITY_CATEGORIES;
        List<RecommendationResultDto> covered = new ArrayList<>(candidates);
        for (String target : targets) {
            PlaceRole targetRole = role(target);
            if (targetRole == PlaceRole.OTHER
                    || covered.stream().anyMatch(candidate -> role(candidate) == targetRole)) {
                continue;
            }
            RecommendationRequest categoryRequest = new RecommendationRequest(
                    request.originPlaceId(), originLatitude, originLongitude,
                    List.of(target), request.district(), request.availableMinutes(), request.budget(),
                    0, 10, request.randomSeed(), CategorySelectionMode.ONLY);
            List<RecommendationResultDto> supplemental = recommendations.recommend(categoryRequest).recommendations()
                    .stream().map(candidate -> normalizeSupplemental(candidate, request.categories())).toList();
            covered = mergeCandidates(covered, supplemental);
        }
        return covered;
    }

    private RecommendationResultDto normalizeSupplemental(RecommendationResultDto candidate,
            List<String> actualPreferences) {
        if (actualPreferences == null || actualPreferences.isEmpty()
                || actualPreferences.stream().map(this::role).anyMatch(role(candidate)::equals)) {
            return candidate;
        }
        RecommendationScoreBreakdown source = candidate.scoreBreakdown();
        double denominator = weights.getCategory() + weights.getDistance()
                + weights.getDiversity() + weights.getDataQuality();
        double numerator = source.distance() * weights.getDistance()
                + source.diversity() * weights.getDiversity()
                + source.dataQuality() * weights.getDataQuality();
        if (source.transitIncluded()) {
            numerator += source.transitConnectivity() * weights.getTransit();
            denominator += weights.getTransit();
        }
        double score = denominator == 0 ? 0 : Math.round(numerator / denominator * 100.0) / 100.0;
        RecommendationScoreBreakdown breakdown = new RecommendationScoreBreakdown(
                0, source.distance(), source.diversity(), source.dataQuality(),
                source.transitConnectivity(), source.transitIncluded());
        List<String> reasons = candidate.recommendationReasons().stream()
                .filter(reason -> !reason.contains("선호 카테고리"))
                .toList();
        return new RecommendationResultDto(candidate.placeId(), candidate.placeName(), candidate.category(),
                candidate.address(), candidate.latitude(), candidate.longitude(), score, breakdown, reasons,
                candidate.distanceMeters(), candidate.transitVerificationStatus(), candidate.dataLimitations(),
                candidate.imageUrl());
    }

    private List<String> missingDiversityTypes(List<RecommendationResultDto> candidates) {
        Set<PlaceRole> roles = new LinkedHashSet<>();
        candidates.forEach(candidate -> roles.add(role(candidate)));
        List<String> missing = new ArrayList<>();
        if (!roles.contains(PlaceRole.SIGHTSEEING)) missing.add("관광지");
        if (!roles.contains(PlaceRole.CAFE)) missing.add("카페");
        if (!roles.contains(PlaceRole.MEAL)) missing.add("음식점");
        if (!roles.contains(PlaceRole.CULTURE)) missing.add("문화시설");
        return missing;
    }

    private List<RecommendationResultDto> select(List<RecommendationResultDto> candidates,
            int requestedCount, int randomness, Long requestedSeed,
            CategorySelectionMode categoryMode, List<String> requestedCategories) {
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

        if ((categoryMode == CategorySelectionMode.REQUIRED || categoryMode == CategorySelectionMode.PREFERRED)
                && requestedCategories != null) {
            for (String requestedCategory : requestedCategories) {
                if (selected.size() >= count) break;
                PlaceRole requiredRole = role(requestedCategory);
                RecommendationResultDto required = available.stream()
                        .filter(candidate -> role(candidate) == requiredRole)
                        .max(Comparator.comparingDouble(RecommendationResultDto::score)
                                .thenComparing(item -> -item.placeId()))
                        .orElse(null);
                if (required != null) {
                    selected.add(required);
                    selectedRoles.add(requiredRole);
                    available.remove(required);
                }
            }
        }

        while (selected.size() < count && !available.isEmpty()) {
            List<RecommendationResultDto> diverse = available.stream()
                    .filter(candidate -> role(candidate) != PlaceRole.OTHER)
                    .filter(candidate -> !selectedRoles.contains(role(candidate)))
                    .toList();
            List<RecommendationResultDto> choicePool = categoryMode == CategorySelectionMode.ONLY || diverse.isEmpty()
                    ? available : diverse;
            RecommendationResultDto chosen;
            if (randomness == 0) {
                chosen = choicePool.stream().max(Comparator
                        .comparingDouble((RecommendationResultDto item) -> deterministicWeight(item, selectedRoles))
                        .thenComparingDouble(item -> -item.distanceMeters())
                        .thenComparing(item -> -item.placeId())).orElseThrow();
            } else {
                int remaining = count - selected.size();
                int poolSize = Math.min(choicePool.size(), remaining + (int) Math.ceil(
                        (choicePool.size() - remaining) * randomness / 100.0));
                List<RecommendationResultDto> pool = choicePool.subList(0, poolSize);
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
        return role(categoryKey(item));
    }

    private PlaceRole role(String category) {
        String key = category == null ? "" : category.trim().toLowerCase(Locale.ROOT);
        if (key.contains("음식") || key.contains("식사") || key.contains("restaurant") || key.equals("food")) {
            return PlaceRole.MEAL;
        }
        if (key.contains("카페") || key.contains("디저트") || key.contains("cafe")) return PlaceRole.CAFE;
        if (key.contains("문화") || key.contains("박물관") || key.contains("전시") || key.contains("culture")) {
            return PlaceRole.CULTURE;
        }
        if (key.contains("공원") || key.contains("산책") || key.contains("갈맷길") || key.contains("walking")) {
            return PlaceRole.WALKING;
        }
        if (key.contains("관광") || key.contains("명소") || key.contains("attraction")) {
            return PlaceRole.SIGHTSEEING;
        }
        return PlaceRole.OTHER;
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
