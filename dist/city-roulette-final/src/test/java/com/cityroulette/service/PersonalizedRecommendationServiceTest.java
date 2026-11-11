package com.cityroulette.service;

import com.cityroulette.domain.Place;
import com.cityroulette.dto.bus.TransitDataCoverageStatus;
import com.cityroulette.dto.bus.TransitRouteCandidate;
import com.cityroulette.dto.bus.TransitRoutePlanResponse;
import com.cityroulette.dto.bus.TransitRouteType;
import com.cityroulette.dto.bus.TransitVerificationStatus;
import com.cityroulette.dto.recommendation.RecommendationRequest;
import com.cityroulette.dto.recommendation.RecommendationTransitStatus;
import com.cityroulette.repository.PlaceRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class PersonalizedRecommendationServiceTest {
    @Autowired PersonalizedRecommendationService service;
    @Autowired PlaceRepository places;
    @MockitoBean TransitRoutePlannerService transitPlanner;

    @BeforeEach void clean() { places.deleteAll(); }

    @Test void filtersCategoryAndDistrictInDatabase() {
        place("해운대 명소", "관광지", "해운대구", 35.160, 129.160);
        place("해운대 카페", "카페", "해운대구", 35.161, 129.161);
        place("중구 명소", "관광지", "중구", 35.100, 129.030);

        var response = service.recommend(request(null, 35.159, 129.159,
                List.of("ATTRACTION"), "해운대구", null, null, 0, 10, 1L));

        assertThat(response.recommendations()).extracting(x -> x.placeName())
                .containsExactly("해운대 명소");
        assertThat(response.candidateCount()).isEqualTo(1);
        assertThat(response.insufficientCandidates()).isTrue();
    }

    @Test void ranksCloserPlacesHigherAndKeepsScoresInRange() {
        Place near = place("가까운 곳", "관광지", "중구", 35.1001, 129.0301);
        place("먼 곳", "관광지", "중구", 35.3000, 129.3000);

        var response = service.recommend(request(null, 35.1000, 129.0300,
                List.of(), null, null, null, 0, 2, 1L));

        assertThat(response.recommendations().get(0).placeId()).isEqualTo(near.getId());
        assertThat(response.recommendations()).allSatisfy(x -> assertThat(x.score()).isBetween(0.0, 100.0));
    }

    @Test void fixedRandomSeedIsReproducibleAndReturnsDiverseCandidates() {
        for (int i = 0; i < 12; i++) {
            place("장소 " + i, i % 2 == 0 ? "관광지" : "카페", "중구",
                    35.10 + i * 0.001, 129.03 + i * 0.001);
        }
        RecommendationRequest request = request(null, 35.10, 129.03,
                List.of(), null, null, null, 100, 5, 42L);

        var first = service.recommend(request);
        var second = service.recommend(request);

        assertThat(first.recommendations()).extracting(x -> x.placeId())
                .containsExactlyElementsOf(second.recommendations().stream().map(x -> x.placeId()).toList());
        assertThat(first.recommendations()).extracting(x -> x.category()).contains("관광지", "카페");
    }

    @Test void reportsUnsupportedTimeAndBudgetWithoutFilteringRecommendations() {
        place("조건 장소", "관광지", "중구", 35.10, 129.03);

        var response = service.recommend(request(null, 35.11, 129.04,
                List.of(), null, 180, 0L, 0, 1, 1L));

        assertThat(response.recommendations()).hasSize(1);
        assertThat(response.dataLimitations()).anyMatch(x -> x.contains("방문 소요시간"));
        assertThat(response.dataLimitations()).anyMatch(x -> x.contains("가격 데이터"));
    }

    @Test void verifiedTransitContributesOnlyWhenPlannerReturnsVerifiedCandidate() {
        Place origin = place("출발", "관광지", "중구", 35.10, 129.03);
        Place destination = place("도착", "카페", "중구", 35.11, 129.04);
        TransitRouteCandidate candidate = new TransitRouteCandidate(origin.getId(), destination.getId(),
                TransitRouteType.DIRECT, 0, null, null, null, List.of("100"), List.of("R100"),
                2, 100, TransitVerificationStatus.VERIFIED, TransitDataCoverageStatus.PARTIAL_DATA);
        when(transitPlanner.plan(eq(origin.getId()), eq(destination.getId()), anyInt(), eq(1)))
                .thenReturn(new TransitRoutePlanResponse(origin.getId(), destination.getId(), 1000,
                        TransitVerificationStatus.VERIFIED, TransitDataCoverageStatus.PARTIAL_DATA,
                        "부분 구간 검증", "HAVERSINE", List.of(candidate)));

        var response = service.recommend(request(origin.getId(), null, null,
                List.of(), null, null, null, 0, 1, 1L));

        assertThat(response.recommendations()).singleElement().satisfies(result -> {
            assertThat(result.transitVerificationStatus()).isEqualTo(RecommendationTransitStatus.VERIFIED);
            assertThat(result.scoreBreakdown().transitIncluded()).isTrue();
            assertThat(result.recommendationReasons()).anyMatch(x -> x.contains("버스"));
        });
    }

    @Test void partialTransitDataDoesNotRemoveCandidateOrClaimUnreachable() {
        Place origin = place("부분 출발", "관광지", "중구", 35.10, 129.03);
        Place destination = place("부분 도착", "카페", "중구", 35.11, 129.04);
        when(transitPlanner.plan(anyLong(), anyLong(), anyInt(), eq(1)))
                .thenReturn(new TransitRoutePlanResponse(origin.getId(), destination.getId(), 1000,
                        TransitVerificationStatus.NO_CANDIDATE, TransitDataCoverageStatus.PARTIAL_DATA,
                        "현재 데이터 내 후보 없음", "HAVERSINE", List.of()));

        var response = service.recommend(request(origin.getId(), null, null,
                List.of(), null, null, null, 0, 1, 1L));

        assertThat(response.recommendations()).singleElement().satisfies(result -> {
            assertThat(result.transitVerificationStatus()).isEqualTo(RecommendationTransitStatus.PARTIAL_DATA);
            assertThat(result.scoreBreakdown().transitIncluded()).isFalse();
            assertThat(result.dataLimitations()).anyMatch(x -> x.contains("이동 불가로 판단하지"));
        });
    }

    @Test void validatesOriginConflictMissingCoordinatesAndCandidatePoolLimit() {
        Place missing = places.save(new Place("좌표 없음", "관광지", "부산", null, null, false));
        assertThatThrownBy(() -> service.recommend(request(missing.getId(), 35.1, 129.1,
                List.of(), null, null, null, 0, 1, 1L))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.recommend(request(missing.getId(), null, null,
                List.of(), null, null, null, 0, 1, 1L))).isInstanceOf(PlaceCoordinatesMissingException.class);

        for (int i = 0; i < 60; i++) place("제한 " + i, "관광지", "중구", 35.1 + i * .0001, 129.1);
        var response = service.recommend(request(null, 35.1, 129.1,
                List.of(), null, null, null, 0, 20, 1L));
        assertThat(response.candidateCount()).isLessThanOrEqualTo(200);
        assertThat(response.recommendations()).hasSize(20);
    }

    private RecommendationRequest request(Long id, Double lat, Double lon, List<String> categories,
            String district, Integer minutes, Long budget, Integer randomness, Integer limit, Long seed) {
        return new RecommendationRequest(id, lat, lon, categories, district,
                minutes, budget, randomness, limit, seed);
    }

    private Place place(String name, String category, String district, double lat, double lon) {
        Place place = new Place(Place.SOURCE_MANUAL, null, name, category, "부산 " + district,
                district, lat, lon, "설명", false);
        return places.save(place);
    }
}
