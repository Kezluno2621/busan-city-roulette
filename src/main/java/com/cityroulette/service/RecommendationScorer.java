package com.cityroulette.service;

import com.cityroulette.config.RecommendationWeights;
import com.cityroulette.config.RecommendationSettings;
import com.cityroulette.domain.Place;
import com.cityroulette.dto.recommendation.RecommendationScoreBreakdown;
import com.cityroulette.dto.recommendation.RecommendationTransitStatus;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/** 추천 후보의 검증 가능한 요소만 가중 평균해 0~100점으로 환산한다. */
@Component
public class RecommendationScorer {
    private final RecommendationWeights weights;
    private final RecommendationSettings settings;
    private final RecommendationCandidateFilter categories;

    public RecommendationScorer(RecommendationWeights weights, RecommendationSettings settings,
            RecommendationCandidateFilter categories) {
        this.weights = weights;
        this.settings = settings;
        this.categories = categories;
    }

    public ScoredCandidate score(Place place, double distanceMeters, Set<String> preferred,
            Map<String, Long> categoryCounts, int candidateCount, RecommendationTransitStatus transitStatus) {
        String normalizedCategory = categories.normalizeCategory(place.getCategory());
        double category = preferred.isEmpty() || preferred.contains(normalizedCategory) ? 100 : 0;
        double maximumDistance = settings.limits().maxDistanceMeters();
        double distance = clamp(100 * (1 - Math.min(distanceMeters, maximumDistance) / maximumDistance));
        long count = categoryCounts.getOrDefault(normalizedCategory, 1L);
        double diversity = candidateCount <= 1 ? 100
                : clamp(100 * (1 - (count - 1.0) / candidateCount));
        double quality = dataQuality(place);
        boolean transitIncluded = transitStatus == RecommendationTransitStatus.VERIFIED;
        double transit = transitIncluded ? 100 : 0;

        double numerator = category * weights.category() + distance * weights.distance()
                + diversity * weights.diversity() + quality * weights.dataQuality();
        double denominator = weights.category() + weights.distance()
                + weights.diversity() + weights.dataQuality();
        if (transitIncluded) {
            numerator += transit * weights.transit();
            denominator += weights.transit();
        }
        double total = denominator == 0 ? 0 : round(numerator / denominator);
        return new ScoredCandidate(place, distanceMeters, total,
                new RecommendationScoreBreakdown(round(category), round(distance), round(diversity),
                        round(quality), round(transit), transitIncluded), transitStatus);
    }

    private double dataQuality(Place place) {
        double score = 60;
        if (present(place.getAddress())) score += 20;
        if (present(place.getDescription())) score += 10;
        if (present(place.getImageUrl())) score += 10;
        return clamp(score);
    }

    private boolean present(String value) {
        return value != null && !value.isBlank();
    }

    private double clamp(double value) {
        return Math.max(0, Math.min(100, value));
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    public record ScoredCandidate(Place place, double distanceMeters, double score,
            RecommendationScoreBreakdown breakdown, RecommendationTransitStatus transitStatus) {
    }
}
