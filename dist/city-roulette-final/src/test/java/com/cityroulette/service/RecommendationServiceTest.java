package com.cityroulette.service;

import com.cityroulette.dto.place.TransitPlaceCandidateDto;
import com.cityroulette.dto.recommendation.RecommendationDto;
import com.cityroulette.dto.recommendation.RecommendationResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RecommendationServiceTest {

    private static final double LATITUDE = 35.1160;
    private static final double LONGITUDE = 128.9680;

    private final PlaceTransitEvaluationService evaluationService =
            mock(PlaceTransitEvaluationService.class);
    private final RecommendationService service = new RecommendationService(evaluationService);

    @Test
    void calculatesRoundTripStayAndRequiredMinutes() {
        when(evaluationService.evaluateCandidates(LATITUDE, LONGITUDE, 3))
                .thenReturn(List.of(candidate("감천문화마을", true, 30)));

        RecommendationResponse response = service.recommend(
                LATITUDE, LONGITUDE, 180, 60, 3);

        assertThat(response.recommended()).isTrue();
        RecommendationDto recommendation = response.recommendation();
        assertThat(recommendation.oneWayTransitMinutes()).isEqualTo(30);
        assertThat(recommendation.estimatedRoundTripMinutes()).isEqualTo(60);
        assertThat(recommendation.stayMinutes()).isEqualTo(60);
        assertThat(recommendation.estimatedRequiredMinutes()).isEqualTo(120);
        assertThat(recommendation.availableMinutes()).isEqualTo(180);
        verify(evaluationService).evaluateCandidates(LATITUDE, LONGITUDE, 3);
    }

    @Test
    void excludesCandidateThatRequiresMoreThanAvailableTime() {
        when(evaluationService.evaluateCandidates(LATITUDE, LONGITUDE, 3))
                .thenReturn(List.of(candidate("시간 초과 장소", true, 30)));

        RecommendationResponse response = service.recommend(
                LATITUDE, LONGITUDE, 100, 60, 3);

        assertThat(response.recommended()).isFalse();
        assertThat(response.recommendation()).isNull();
    }

    @Test
    void excludesUnavailableAndMissingTimeCandidates() {
        when(evaluationService.evaluateCandidates(LATITUDE, LONGITUDE, 3))
                .thenReturn(List.of(
                        candidate("경로 없는 장소", false, null),
                        candidate("시간 없는 장소", true, null)));

        RecommendationResponse response = service.recommend(
                LATITUDE, LONGITUDE, 180, 60, 3);

        assertThat(response.recommended()).isFalse();
        assertThat(response.message()).contains("찾지 못했습니다");
    }

    @Test
    void returnsNoRecommendationWhenCandidateListIsEmpty() {
        when(evaluationService.evaluateCandidates(LATITUDE, LONGITUDE, 3))
                .thenReturn(List.of());

        RecommendationResponse response = service.recommend(
                LATITUDE, LONGITUDE, 180, 60, 3);

        assertThat(response.recommended()).isFalse();
        assertThat(response.recommendation()).isNull();
    }

    @Test
    void selectsTheOnlyEligibleCandidate() {
        when(evaluationService.evaluateCandidates(LATITUDE, LONGITUDE, 3))
                .thenReturn(List.of(
                        candidate("시간 초과", true, 90),
                        candidate("추천 가능", true, 30),
                        candidate("경로 없음", false, null)));

        RecommendationResponse response = service.recommend(
                LATITUDE, LONGITUDE, 180, 60, 3);

        assertThat(response.recommended()).isTrue();
        assertThat(response.recommendation().name()).isEqualTo("추천 가능");
    }

    @Test
    void randomSelectionAlwaysComesFromEligibleCandidates() {
        when(evaluationService.evaluateCandidates(LATITUDE, LONGITUDE, 3))
                .thenReturn(List.of(
                        candidate("가능 A", true, 20),
                        candidate("가능 B", true, 30),
                        candidate("시간 초과", true, 100)));

        RecommendationResponse response = service.recommend(
                LATITUDE, LONGITUDE, 180, 60, 3);

        assertThat(response.recommended()).isTrue();
        assertThat(response.recommendation().name()).isIn("가능 A", "가능 B");
        verify(evaluationService).evaluateCandidates(LATITUDE, LONGITUDE, 3);
    }

    private TransitPlaceCandidateDto candidate(
            String name,
            Boolean transitAvailable,
            Integer totalTime) {
        return new TransitPlaceCandidateDto(
                1L,
                "external-1",
                name,
                "관광지",
                "부산광역시",
                35.1,
                129.0,
                4.3,
                totalTime,
                620,
                1550,
                1,
                0,
                "출발 정류장",
                "도착 정류장",
                2,
                transitAvailable);
    }
}
