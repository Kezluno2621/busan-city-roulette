package com.cityroulette.service;

import com.cityroulette.domain.Place;
import com.cityroulette.dto.bus.TransitDataCoverageStatus;
import com.cityroulette.dto.bus.TransitRoutePlanResponse;
import com.cityroulette.dto.bus.TransitVerificationStatus;
import com.cityroulette.dto.course.CourseEstimateStatus;
import com.cityroulette.dto.course.CourseGenerationMode;
import com.cityroulette.dto.course.CourseGenerationRequest;
import com.cityroulette.dto.recommendation.CategorySelectionMode;
import com.cityroulette.dto.recommendation.PersonalizedRecommendationResponse;
import com.cityroulette.dto.recommendation.RecommendationRequest;
import com.cityroulette.dto.recommendation.RecommendationResultDto;
import com.cityroulette.dto.recommendation.RecommendationScoreBreakdown;
import com.cityroulette.dto.recommendation.RecommendationTransitStatus;
import com.cityroulette.repository.PlaceRepository;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.LongStream;
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
class CourseGenerationServiceTest {
    @Autowired CourseGenerationService courses;
    @Autowired PlaceRepository places;
    @MockitoBean PersonalizedRecommendationService recommendations;
    @MockitoBean TransitRoutePlannerService transitPlanner;

    @BeforeEach void clean() { places.deleteAll(); }

    @Test void randomnessZeroIsDeterministicAndFavorsCategoryDiversity() {
        stub(List.of(
                result(1, "명소 A", "관광지", 90, 35.20, 129.20),
                result(2, "명소 B", "관광지", 89, 35.11, 129.11),
                result(3, "카페 C", "카페", 88, 35.12, 129.12)));
        CourseGenerationRequest request = request(0, 2, 42L);

        var first = courses.generate(request);
        var second = courses.generate(request);

        assertThat(first.places()).extracting(x -> x.placeId())
                .containsExactlyElementsOf(second.places().stream().map(x -> x.placeId()).toList())
                .containsExactly(3L, 1L);
        assertThat(first.places()).extracting(x -> x.category()).containsExactlyInAnyOrder("관광지", "카페");
    }

    @Test void randomnessHundredVariesAcrossSeedsButSameSeedIsReproducible() {
        stub(LongStream.rangeClosed(1, 12)
                .mapToObj(id -> result(id, "장소 " + id, id % 2 == 0 ? "관광지" : "카페",
                        100 - id, 35.10 + id * .001, 129.10 + id * .001)).toList());
        Set<List<Long>> selections = new LinkedHashSet<>();
        for (long seed = 1; seed <= 8; seed++) {
            selections.add(ids(courses.generate(request(100, 4, seed))));
        }

        assertThat(selections.size()).isGreaterThan(1);
        assertThat(ids(courses.generate(request(100, 4, 7L))))
                .containsExactlyElementsOf(ids(courses.generate(request(100, 4, 7L))));
    }

    @Test void preventsDuplicatePlacesAndReportsCandidateShortage() {
        RecommendationResultDto duplicate = result(1, "중복", "관광지", 90, 35.11, 129.11);
        RecommendationResultDto missingCoordinates = new RecommendationResultDto(3L, "좌표 없음", "관광지", "부산",
                null, null, 99, new RecommendationScoreBreakdown(100, 0, 80, 60, 0, false),
                List.of(), 0, RecommendationTransitStatus.NOT_EVALUATED, List.of());
        stub(List.of(duplicate, duplicate, result(2, "유일", "카페", 80, 35.12, 129.12), missingCoordinates));

        var response = courses.generate(request(0, 5, 1L));

        assertThat(response.actualPlaceCount()).isEqualTo(2);
        assertThat(response.requestedPlaceCount()).isEqualTo(5);
        assertThat(response.insufficientCandidates()).isTrue();
        assertThat(response.places()).extracting(x -> x.placeId()).doesNotHaveDuplicates();
    }

    @Test void ordersSelectedPlacesByNearestNeighborAndCalculatesLegDistance() {
        stub(List.of(
                result(1, "먼 곳", "관광지", 100, 35.30, 129.30),
                result(2, "가까운 곳", "카페", 99, 35.101, 129.101),
                result(3, "중간 곳", "문화시설", 98, 35.15, 129.15)));

        var response = courses.generate(request(0, 3, 1L));

        assertThat(response.places()).extracting(x -> x.placeId()).containsExactly(2L, 3L, 1L);
        assertThat(response.places()).extracting(x -> x.sequence()).containsExactly(1, 2, 3);
        assertThat(response.places().get(0).distanceFromPreviousMeters()).isBetween(100.0, 200.0);
        assertThat(response.places()).allSatisfy(x -> {
            assertThat(x.estimatedTimeStatus()).isEqualTo(CourseEstimateStatus.UNKNOWN);
            assertThat(x.estimatedCostStatus()).isEqualTo(CourseEstimateStatus.UNKNOWN);
        });
    }

    @Test void minimizesConsecutiveMealsWhenSightseeingAlternativesExist() {
        stub(List.of(
                result(1, "식당 A", "음식점", 100, 35.101, 129.101),
                result(2, "식당 B", "음식점", 99, 35.102, 129.102),
                result(3, "명소 C", "관광지", 98, 35.103, 129.103),
                result(4, "명소 D", "관광지", 97, 35.104, 129.104)));

        var response = courses.generate(request(0, 4, 42L));

        assertThat(response.places()).hasSize(4);
        for (int index = 1; index < response.places().size(); index++) {
            assertThat(List.of(response.places().get(index - 1).category(), response.places().get(index).category()))
                    .isNotEqualTo(List.of("음식점", "음식점"));
        }
        assertThat(response.dataLimitations()).anyMatch(value -> value.contains("음식점 연속 방문을 줄였습니다"));
    }

    @Test void generatesCourseNormallyWhenAllCandidatesAreMeals() {
        stub(List.of(
                result(1, "식당 A", "음식점", 100, 35.101, 129.101),
                result(2, "식당 B", "음식점", 99, 35.102, 129.102),
                result(3, "식당 C", "음식점", 98, 35.103, 129.103)));

        var response = courses.generate(request(0, 3, 42L));

        assertThat(response.places()).hasSize(3).allMatch(place -> place.category().equals("음식점"));
        assertThat(response.dataLimitations()).anyMatch(value -> value.contains("음식점이 연속될 수 있습니다"));
    }

    @Test void propagatesOnlyServerApprovedImageFromRecommendation() {
        RecommendationResultDto withImage = new RecommendationResultDto(1L, "명소", "관광지", "부산",
                35.101, 129.101, 90, new RecommendationScoreBreakdown(100, 80, 80, 80, 0, false),
                List.of(), 100, RecommendationTransitStatus.NOT_EVALUATED, List.of(),
                "https://example.org/place.jpg");
        stub(List.of(withImage));

        var response = courses.generate(request(0, 1, 42L));

        assertThat(response.places()).singleElement()
                .extracting(place -> place.imageUrl()).isEqualTo("https://example.org/place.jpg");
    }

    @Test void keepsCourseWhenTransitCoverageIsPartial() {
        Place origin = places.save(new Place("출발", "관광지", "부산", 35.10, 129.10, false));
        stub(List.of(result(10, "도착", "카페", 90, 35.11, 129.11)));
        when(transitPlanner.plan(eq(origin.getId()), eq(10L), eq(1000), eq(1)))
                .thenReturn(new TransitRoutePlanResponse(origin.getId(), 10L, 1000,
                        TransitVerificationStatus.NO_CANDIDATE, TransitDataCoverageStatus.PARTIAL_DATA,
                        "부분 데이터", "HAVERSINE", List.of()));
        CourseGenerationRequest request = new CourseGenerationRequest(origin.getId(), null, null,
                List.of(), null, null, null, 0, 1, 1L);

        var response = courses.generate(request);

        assertThat(response.places()).singleElement().satisfies(place -> {
            assertThat(place.transitVerificationStatus()).isEqualTo(RecommendationTransitStatus.PARTIAL_DATA);
            assertThat(place.dataLimitations()).anyMatch(x -> x.contains("단정하지"));
        });
    }

    @Test void rejectsInvalidOriginAndMissingOriginCoordinates() {
        assertThatThrownBy(() -> courses.generate(new CourseGenerationRequest(
                999L, null, null, List.of(), null, null, null, 0, 1, 1L)))
                .isInstanceOf(PlaceNotFoundException.class);
        Place missing = places.save(new Place("좌표 없음", "관광지", "부산", null, null, false));
        assertThatThrownBy(() -> courses.generate(new CourseGenerationRequest(
                missing.getId(), null, null, List.of(), null, null, null, 0, 1, 1L)))
                .isInstanceOf(PlaceCoordinatesMissingException.class);
        assertThatThrownBy(() -> courses.generate(new CourseGenerationRequest(
                1L, 35.1, 129.1, List.of(), null, null, null, 0, 1, 1L)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test void instantModeUsesBusanDefaultOriginWhenDepartureIsOmitted() {
        stub(List.of(result(1, "즉흥 명소", "관광지", 90, 35.18, 129.08)));
        CourseGenerationRequest request = new CourseGenerationRequest(null, null, null,
                List.of(), null, null, null, 100, 1, 7L,
                CategorySelectionMode.PREFERRED, CourseGenerationMode.INSTANT, List.of());

        var response = courses.generate(request);

        assertThat(response.origin().name()).isEqualTo("부산 중심 출발");
        assertThat(response.places()).hasSize(1);
        assertThat(response.courseTitle()).contains("즉흥 룰렛");
    }

    @Test void defaultRandomRunAvoidsRecentPlacesWhenFreshCandidatesAreEnough() {
        stub(List.of(
                result(1, "최근 A", "관광지", 100, 35.11, 129.11),
                result(2, "최근 B", "카페", 99, 35.12, 129.12),
                result(3, "새 장소 C", "문화시설", 98, 35.13, 129.13),
                result(4, "새 장소 D", "음식점", 97, 35.14, 129.14)));
        CourseGenerationRequest request = new CourseGenerationRequest(null, 35.10, 129.10,
                List.of(), null, null, null, 100, 2, null,
                CategorySelectionMode.PREFERRED, CourseGenerationMode.INSTANT, List.of(1L, 2L));

        var response = courses.generate(request);

        assertThat(response.places()).extracting(x -> x.placeId()).containsExactlyInAnyOrder(3L, 4L);
    }

    @Test void fixedSeedKeepsReproducibilityEvenWithRecentHistory() {
        stub(List.of(
                result(1, "장소 A", "관광지", 100, 35.11, 129.11),
                result(2, "장소 B", "카페", 99, 35.12, 129.12),
                result(3, "장소 C", "문화시설", 98, 35.13, 129.13)));
        CourseGenerationRequest first = new CourseGenerationRequest(null, 35.10, 129.10,
                List.of(), null, null, null, 100, 2, 42L,
                CategorySelectionMode.PREFERRED, CourseGenerationMode.INSTANT, List.of(1L, 2L));
        CourseGenerationRequest second = new CourseGenerationRequest(null, 35.10, 129.10,
                List.of(), null, null, null, 100, 2, 42L,
                CategorySelectionMode.PREFERRED, CourseGenerationMode.INSTANT, List.of());

        assertThat(ids(courses.generate(first))).containsExactlyElementsOf(ids(courses.generate(second)));
    }

    @Test void requiredModeIncludesRequestedMealWhenCandidateExists() {
        stub(List.of(
                result(1, "명소 A", "관광지", 100, 35.11, 129.11),
                result(2, "명소 B", "관광지", 99, 35.12, 129.12),
                result(3, "식당 C", "음식점", 70, 35.13, 129.13)));
        CourseGenerationRequest request = new CourseGenerationRequest(null, 35.10, 129.10,
                List.of("RESTAURANT"), null, null, null, 0, 2, 1L,
                CategorySelectionMode.REQUIRED, CourseGenerationMode.PERSONALIZED, List.of());

        assertThat(courses.generate(request).places()).extracting(x -> x.category()).contains("음식점");
    }

    @Test void preferredMealModeBuildsMixedCourseWhenOtherRolesExist() {
        stub(List.of(
                result(1, "식당 A", "음식점", 100, 35.11, 129.11),
                result(2, "식당 B", "음식점", 99, 35.12, 129.12),
                result(3, "명소 C", "관광지", 82, 35.13, 129.13),
                result(4, "카페 D", "카페", 80, 35.14, 129.14),
                result(5, "문화 E", "문화시설", 78, 35.15, 129.15)));
        CourseGenerationRequest request = new CourseGenerationRequest(null, 35.10, 129.10,
                List.of("RESTAURANT"), null, null, null, 50, 4, 7L,
                CategorySelectionMode.PREFERRED, CourseGenerationMode.PERSONALIZED, List.of());

        var response = courses.generate(request);

        assertThat(response.places()).hasSize(4);
        assertThat(response.places()).extracting(x -> x.category())
                .contains("음식점", "관광지", "카페", "문화시설");
    }

    @Test void preferredModeLoadsSmallSamplesForRareMissingCategories() {
        when(recommendations.recommend(argThat(request -> request != null
                && request.categories().equals(List.of("RESTAURANT"))
                && request.categoryMode() == CategorySelectionMode.PREFERRED)))
                .thenReturn(response(List.of(result(1, "식당", "음식점", 100, 35.11, 129.11))));
        when(recommendations.recommend(argThat(request -> request != null && request.categories().isEmpty())))
                .thenReturn(response(List.of(
                        result(1, "식당", "음식점", 100, 35.11, 129.11),
                        result(2, "명소", "관광지", 80, 35.12, 129.12))));
        when(recommendations.recommend(argThat(request -> request != null
                && request.categories().equals(List.of("CAFE"))
                && request.categoryMode() == CategorySelectionMode.ONLY)))
                .thenReturn(response(List.of(result(3, "희소 카페", "카페", 70, 35.13, 129.13))));
        when(recommendations.recommend(argThat(request -> request != null
                && request.categories().equals(List.of("CULTURE"))
                && request.categoryMode() == CategorySelectionMode.ONLY)))
                .thenReturn(response(List.of(result(4, "희소 문화", "문화시설", 60, 35.14, 129.14))));
        CourseGenerationRequest request = new CourseGenerationRequest(null, 35.10, 129.10,
                List.of("RESTAURANT"), null, null, null, 50, 4, 7L,
                CategorySelectionMode.PREFERRED, CourseGenerationMode.PERSONALIZED, List.of());

        assertThat(courses.generate(request).places()).extracting(x -> x.category())
                .containsExactlyInAnyOrder("음식점", "관광지", "카페", "문화시설");
    }

    private CourseGenerationRequest request(int randomness, int count, Long seed) {
        return new CourseGenerationRequest(null, 35.10, 129.10, List.of(), null,
                180, 30_000L, randomness, count, seed);
    }

    private void stub(List<RecommendationResultDto> candidates) {
        when(recommendations.recommend(any())).thenReturn(response(candidates));
    }

    private PersonalizedRecommendationResponse response(List<RecommendationResultDto> candidates) {
        return new PersonalizedRecommendationResponse(
                candidates, candidates.size(), false, "fixture", List.of("fixture limitation"));
    }

    private RecommendationResultDto result(long id, String name, String category,
            double score, double latitude, double longitude) {
        return new RecommendationResultDto(id, name, category, "부산", latitude, longitude, score,
                new RecommendationScoreBreakdown(100, 80, 80, 80, 0, false),
                List.of("추천 점수 기반"), 1000, RecommendationTransitStatus.NOT_EVALUATED, List.of());
    }

    private List<Long> ids(com.cityroulette.dto.course.CourseGenerationResponse response) {
        return response.places().stream().map(x -> x.placeId()).toList();
    }
}
