package com.cityroulette.service;

import com.cityroulette.dto.place.TransitPlaceCandidateDto;
import com.cityroulette.dto.recommendation.RecommendationDto;
import com.cityroulette.dto.recommendation.RecommendationResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class RecommendationService {

    private final PlaceTransitEvaluationService evaluationService;

    public RecommendationService(PlaceTransitEvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    public RecommendationResponse recommend(
            double latitude,
            double longitude,
            int availableMinutes,
            int stayMinutes,
            int candidateLimit) {
        List<TransitPlaceCandidateDto> candidates = evaluationService.evaluateCandidates(
                latitude, longitude, candidateLimit);

        List<RecommendationDto> eligibleCandidates = candidates.stream()
                .filter(this::hasUsableTransitRoute)
                .filter(candidate -> isWithinAvailableTime(
                        candidate, availableMinutes, stayMinutes))
                .map(candidate -> toRecommendation(
                        candidate, availableMinutes, stayMinutes))
                .toList();

        if (eligibleCandidates.isEmpty()) {
            return RecommendationResponse.notFound();
        }

        int selectedIndex = ThreadLocalRandom.current().nextInt(eligibleCandidates.size());
        return RecommendationResponse.success(eligibleCandidates.get(selectedIndex));
    }

    private boolean hasUsableTransitRoute(TransitPlaceCandidateDto candidate) {
        return Boolean.TRUE.equals(candidate.transitAvailable())
                && candidate.totalTime() != null
                && candidate.totalTime() >= 0;
    }

    private boolean isWithinAvailableTime(
            TransitPlaceCandidateDto candidate,
            int availableMinutes,
            int stayMinutes) {
        long requiredMinutes = (long) candidate.totalTime() * 2 + stayMinutes;
        return requiredMinutes <= availableMinutes;
    }

    private RecommendationDto toRecommendation(
            TransitPlaceCandidateDto candidate,
            int availableMinutes,
            int stayMinutes) {
        int roundTripMinutes = candidate.totalTime() * 2;
        int requiredMinutes = roundTripMinutes + stayMinutes;

        return new RecommendationDto(
                candidate.id(),
                candidate.externalId(),
                candidate.name(),
                candidate.category(),
                candidate.address(),
                candidate.latitude(),
                candidate.longitude(),
                candidate.distanceKm(),
                candidate.totalTime(),
                roundTripMinutes,
                stayMinutes,
                requiredMinutes,
                availableMinutes,
                candidate.totalWalk(),
                candidate.payment(),
                candidate.busTransitCount(),
                candidate.subwayTransitCount(),
                candidate.firstStartStation(),
                candidate.lastEndStation(),
                candidate.pathType());
    }
}
