package com.cityroulette.service;

import com.cityroulette.dto.place.TransitPlaceCandidateDto;
import com.cityroulette.dto.place.NearbyPlaceDto;
import com.cityroulette.dto.recommendation.RecommendationDto;
import com.cityroulette.dto.recommendation.RecommendationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class RecommendationService {

    private static final Logger log = LoggerFactory.getLogger(RecommendationService.class);

    private final PlaceTransitEvaluationService evaluationService;
    private final PlaceCandidateService placeCandidateService;

    public RecommendationService(
            PlaceTransitEvaluationService evaluationService,
            PlaceCandidateService placeCandidateService) {
        this.evaluationService = evaluationService;
        this.placeCandidateService = placeCandidateService;
    }

    public RecommendationResponse recommend(
            double latitude,
            double longitude,
            int availableMinutes,
            int stayMinutes,
            int candidateLimit) {
        List<TransitPlaceCandidateDto> candidates;
        try {
            candidates = evaluationService.evaluateCandidates(
                    latitude, longitude, candidateLimit);
        } catch (OdsayApiException exception) {
            log.warn("ODsay route lookup failed; using place-only fallback: {}", exception.getMessage());
            return recommendWithoutTransit(
                    latitude, longitude, availableMinutes, stayMinutes, candidateLimit);
        }

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

    private RecommendationResponse recommendWithoutTransit(
            double latitude,
            double longitude,
            int availableMinutes,
            int stayMinutes,
            int candidateLimit) {
        List<NearbyPlaceDto> candidates = placeCandidateService.findNearestPlaces(
                latitude, longitude, candidateLimit);
        if (candidates.isEmpty()) {
            return RecommendationResponse.notFound();
        }

        NearbyPlaceDto selected = candidates.get(
                ThreadLocalRandom.current().nextInt(candidates.size()));
        RecommendationDto recommendation = new RecommendationDto(
                selected.id(),
                selected.externalId(),
                selected.name(),
                selected.category(),
                selected.address(),
                selected.latitude(),
                selected.longitude(),
                selected.distanceKm(),
                null,
                null,
                stayMinutes,
                null,
                availableMinutes,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
        return RecommendationResponse.successWithoutTransit(recommendation);
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
