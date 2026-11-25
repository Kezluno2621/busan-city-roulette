package com.cityroulette.domain.recommendation.service;

import com.cityroulette.domain.place.service.PlaceCoordinatesMissingException;
import com.cityroulette.domain.place.service.PlaceImagePolicyService;
import com.cityroulette.domain.place.service.PlaceNotFoundException;
import com.cityroulette.domain.transport.service.TransitRoutePlannerService;
import com.cityroulette.domain.recommendation.config.RecommendationWeights;
import com.cityroulette.domain.recommendation.config.RecommendationSettings;
import com.cityroulette.domain.place.entity.Place;
import com.cityroulette.domain.transport.dto.TransitDataCoverageStatus;
import com.cityroulette.domain.transport.dto.TransitRoutePlanResponse;
import com.cityroulette.domain.transport.dto.TransitVerificationStatus;
import com.cityroulette.domain.recommendation.dto.PersonalizedRecommendationResponse;
import com.cityroulette.domain.recommendation.dto.RecommendationRequest;
import com.cityroulette.domain.recommendation.dto.RecommendationResultDto;
import com.cityroulette.domain.recommendation.dto.RecommendationTransitStatus;
import com.cityroulette.domain.place.repository.PlaceRepository;
import com.cityroulette.domain.recommendation.service.RecommendationCandidateFilter.CandidateSelection;
import com.cityroulette.domain.recommendation.service.RecommendationScorer.ScoredCandidate;
import com.cityroulette.global.util.DistanceCalculator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 조건으로 후보를 필터링하고 점수화한 뒤 제한된 랜덤성을 적용하는 추천 서비스입니다.
 *
 * <p>교통 데이터가 부분 적재 상태일 때는 경로가 없다는 이유로 장소를 제거하지 않고,
 * 결과 DTO에 검증 수준과 데이터 한계를 함께 표시합니다.</p>
 */
@Service
@Transactional(readOnly = true)
public class PersonalizedRecommendationService {
    private static final int DEFAULT_RANDOMNESS = 0;

    private final PlaceRepository places;
    private final RecommendationCandidateFilter candidateFilter;
    private final RecommendationScorer scorer;
    private final TransitRoutePlannerService transitPlanner;
    private final RecommendationWeights weights;
    private final RecommendationSettings settings;
    private final PlaceImagePolicyService images;

    public PersonalizedRecommendationService(PlaceRepository places,
            RecommendationCandidateFilter candidateFilter, RecommendationScorer scorer,
            TransitRoutePlannerService transitPlanner, RecommendationWeights weights,
            RecommendationSettings settings, PlaceImagePolicyService images) {
        this.places = places;
        this.candidateFilter = candidateFilter;
        this.scorer = scorer;
        this.transitPlanner = transitPlanner;
        this.weights = weights;
        this.settings = settings;
        this.images = images;
    }

    /**
     * 추천 요청을 검증하고 최종 표시 가능한 장소 목록을 생성합니다.
     *
     * @param request 출발지, 선호 카테고리, 지역, 랜덤성, 결과 제한을 담은 요청
     * @return 점수·추천 이유·데이터 한계를 포함한 추천 결과
     */
    public PersonalizedRecommendationResponse recommend(RecommendationRequest request) {
        Origin origin = resolveOrigin(request);
        int limit = request.limit() == null ? settings.defaults().recommendationLimit() : request.limit();
        int randomness = request.randomness() == null ? DEFAULT_RANDOMNESS : request.randomness();
        CandidateSelection selection = candidateFilter.select(request, origin.placeId(), limit);
        List<String> globalLimitations = globalLimitations(request, origin.placeId() == null);
        if (selection.places().isEmpty()) {
            return new PersonalizedRecommendationResponse(List.of(), 0, true,
                    "현재 조건에 맞는 좌표 보유 장소가 없습니다. 카테고리 또는 희망 지역을 조정해주세요.",
                    globalLimitations);
        }

        Map<String, Long> categoryCounts = selection.places().stream().collect(Collectors.groupingBy(
                p -> candidateFilter.normalizeCategory(p.getCategory()), Collectors.counting()));
        List<ScoredCandidate> initial = selection.places().stream()
                .map(place -> scorer.score(place, distance(origin, place), selection.preferredCategories(),
                        categoryCounts, selection.places().size(), RecommendationTransitStatus.NOT_EVALUATED))
                .sorted(scoreOrder()).toList();

        Map<Long, RecommendationTransitStatus> transit = evaluateTransit(origin.placeId(), initial, limit);
        List<ScoredCandidate> scored = initial.stream()
                .map(item -> scorer.score(item.place(), item.distanceMeters(), selection.preferredCategories(),
                        categoryCounts, selection.places().size(),
                        transit.getOrDefault(item.place().getId(), RecommendationTransitStatus.NOT_EVALUATED)))
                .sorted(scoreOrder()).toList();
        List<ScoredCandidate> chosen = choose(scored, limit, randomness, request.randomSeed());
        var displayableImages = images.displayablePlaceIds(chosen.stream()
                .map(item -> item.place().getId()).toList());
        List<RecommendationResultDto> results = chosen.stream()
                .map(item -> toDto(item, selection.preferredCategories(), request, globalLimitations,
                        displayableImages.contains(item.place().getId())))
                .toList();
        boolean insufficient = selection.places().size() < limit;
        String message = insufficient
                ? "조건에 맞는 후보가 요청 수보다 적어 확인 가능한 장소만 반환했습니다."
                : "현재 저장된 장소 데이터에서 조건에 맞는 추천 결과입니다.";
        return new PersonalizedRecommendationResponse(results, selection.places().size(), insufficient,
                message, globalLimitations);
    }

    private Origin resolveOrigin(RecommendationRequest request) {
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
            return new Origin(place.getId(), place.getLatitude(), place.getLongitude());
        }
        return new Origin(null, request.originLatitude(), request.originLongitude());
    }

    private Map<Long, RecommendationTransitStatus> evaluateTransit(Long originPlaceId,
            List<ScoredCandidate> ranked, int requestedLimit) {
        Map<Long, RecommendationTransitStatus> result = new HashMap<>();
        if (originPlaceId == null) return result;
        int evaluationLimit = Math.min(ranked.size(), Math.min(settings.transit().evaluationLimit(),
                Math.max(requestedLimit, requestedLimit * 2)));
        for (int i = 0; i < evaluationLimit; i++) {
            Place destination = ranked.get(i).place();
            try {
                TransitRoutePlanResponse plan = transitPlanner.plan(
                        originPlaceId, destination.getId(), settings.transit().radiusMeters(), 1);
                result.put(destination.getId(), transitStatus(plan));
            } catch (RuntimeException exception) {
                result.put(destination.getId(), RecommendationTransitStatus.UNVERIFIED);
            }
        }
        return result;
    }

    private RecommendationTransitStatus transitStatus(TransitRoutePlanResponse plan) {
        if (plan.verificationStatus() == TransitVerificationStatus.VERIFIED
                && !plan.candidates().isEmpty()) return RecommendationTransitStatus.VERIFIED;
        if (plan.dataCoverageStatus() == TransitDataCoverageStatus.PARTIAL_DATA
                || plan.verificationStatus() == TransitVerificationStatus.PARTIAL_DATA
                || plan.verificationStatus() == TransitVerificationStatus.NO_CANDIDATE) {
            return RecommendationTransitStatus.PARTIAL_DATA;
        }
        return RecommendationTransitStatus.UNVERIFIED;
    }

    private List<ScoredCandidate> choose(List<ScoredCandidate> ranked, int limit,
            int randomness, Long requestedSeed) {
        if (ranked.size() <= limit || randomness == 0) return ranked.stream().limit(limit).toList();
        int poolSize = Math.min(ranked.size(), limit + (int) Math.ceil(
                (ranked.size() - limit) * randomness / 100.0));
        List<ScoredCandidate> pool = new ArrayList<>(ranked.subList(0, poolSize));
        List<ScoredCandidate> selected = new ArrayList<>();
        long seed = requestedSeed == null ? ThreadLocalRandom.current().nextLong() : requestedSeed;
        SplittableRandom random = new SplittableRandom(seed);
        while (!pool.isEmpty() && selected.size() < limit) {
            double total = pool.stream().mapToDouble(x -> x.score() + 1).sum();
            double pick = random.nextDouble(total);
            int chosen = pool.size() - 1;
            for (int i = 0; i < pool.size(); i++) {
                pick -= pool.get(i).score() + 1;
                if (pick <= 0) { chosen = i; break; }
            }
            selected.add(pool.remove(chosen));
        }
        return selected;
    }

    private RecommendationResultDto toDto(ScoredCandidate candidate, java.util.Set<String> preferred,
            RecommendationRequest request, List<String> globalLimitations, boolean imageAllowed) {
        Place place = candidate.place();
        List<String> reasons = new ArrayList<>();
        if (!preferred.isEmpty() && preferred.contains(candidateFilter.normalizeCategory(place.getCategory()))) {
            reasons.add("선호 카테고리와 일치합니다.");
        }
        if (candidate.distanceMeters() <= 5_000) reasons.add("출발지에서 직선거리가 비교적 가깝습니다.");
        if (candidate.breakdown().dataQuality() >= 80) reasons.add("주소와 상세 정보가 비교적 충실합니다.");
        if (candidate.transitStatus() == RecommendationTransitStatus.VERIFIED) {
            reasons.add("현재 적재된 정적 버스 구간에서 연결 후보가 검증되었습니다.");
        }
        if (reasons.isEmpty()) reasons.add("현재 조건과 저장된 데이터 품질을 종합해 선정했습니다.");

        LinkedHashSet<String> limitations = new LinkedHashSet<>(globalLimitations);
        if (candidate.transitStatus() == RecommendationTransitStatus.PARTIAL_DATA) {
            limitations.add("교통 데이터가 부분 적재 상태여서 이동 불가로 판단하지 않았습니다.");
        } else if (candidate.transitStatus() == RecommendationTransitStatus.UNVERIFIED) {
            limitations.add("교통 연결성을 검증하지 못했으며 교통 점수는 제외했습니다.");
        } else if (candidate.transitStatus() == RecommendationTransitStatus.NOT_EVALUATED) {
            limitations.add("쿼리 수 제한을 위해 이 후보의 교통 연결성은 평가하지 않았습니다.");
        }
        return new RecommendationResultDto(place.getId(), place.getName(), place.getCategory(),
                place.getAddress(), place.getLatitude(), place.getLongitude(), candidate.score(),
                candidate.breakdown(), List.copyOf(reasons), round(candidate.distanceMeters()),
                candidate.transitStatus(), List.copyOf(limitations), imageAllowed ? place.getImageUrl() : null);
    }

    private List<String> globalLimitations(RecommendationRequest request, boolean coordinateOrigin) {
        List<String> result = new ArrayList<>();
        result.add("distanceMeters는 실제 도보 경로가 아닌 직선거리입니다.");
        if (request.availableMinutes() != null) result.add("방문 소요시간 데이터가 없어 여행 가능 시간은 평가하지 않았습니다.");
        if (request.budget() != null) result.add("가격 데이터가 없어 예산은 평가하지 않았습니다.");
        if (coordinateOrigin) result.add("좌표 출발지는 Place ID가 없어 정적 버스 경로를 평가하지 않았습니다.");
        return List.copyOf(result);
    }

    private double distance(Origin origin, Place place) {
        return DistanceCalculator.calculateKm(origin.latitude(), origin.longitude(),
                place.getLatitude(), place.getLongitude()) * 1_000;
    }

    private Comparator<ScoredCandidate> scoreOrder() {
        return Comparator.comparingDouble(ScoredCandidate::score).reversed()
                .thenComparingDouble(ScoredCandidate::distanceMeters)
                .thenComparing(x -> x.place().getId());
    }

    private double round(double value) { return Math.round(value * 10.0) / 10.0; }
    private record Origin(Long placeId, double latitude, double longitude) { }
}
