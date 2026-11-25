package com.cityroulette.service;

import com.cityroulette.config.RecommendationSettings;
import com.cityroulette.dto.recommendation.RecommendationResultDto;
import com.cityroulette.util.DistanceCalculator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

/** 선택된 장소의 역할 다양성과 직선거리를 비교해 방문 순서를 정합니다. */
@Component
public class CourseOrderingService {
    private final CourseCandidateSelector categories;
    private final RecommendationSettings settings;

    public CourseOrderingService(CourseCandidateSelector categories, RecommendationSettings settings) {
        this.categories = categories;
        this.settings = settings;
    }

    public List<OrderedCandidate> order(double originLatitude, double originLongitude,
            List<RecommendationResultDto> selected) {
        if (selected.size() < 2) return nearestNeighbor(originLatitude, originLongitude, selected);
        List<OrderedCandidate> baseline = nearestNeighbor(originLatitude, originLongitude, selected);
        double maximumDistance = totalDistance(baseline)
                * settings.course().maxDiversityDistanceFactor()
                + settings.course().maxDiversityDistanceAllowanceMeters();
        List<List<RecommendationResultDto>> permutations = new ArrayList<>();
        permute(new ArrayList<>(), new ArrayList<>(selected), permutations);
        return permutations.stream().map(items -> ordered(originLatitude, originLongitude, items))
                .filter(items -> totalDistance(items) <= maximumDistance)
                .min(Comparator.comparingInt(this::mealAdjacencies)
                        .thenComparingInt(this::sameRoleAdjacencies)
                        .thenComparingDouble(this::totalDistance)
                        .thenComparing(this::orderKey))
                .orElse(baseline);
    }

    public boolean hasMealAdjacency(List<OrderedCandidate> order) {
        return mealAdjacencies(order) > 0;
    }

    private List<OrderedCandidate> nearestNeighbor(double latitude, double longitude,
            List<RecommendationResultDto> selected) {
        List<RecommendationResultDto> remaining = new ArrayList<>(selected);
        List<OrderedCandidate> ordered = new ArrayList<>();
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

    private void permute(List<RecommendationResultDto> prefix, List<RecommendationResultDto> remaining,
            List<List<RecommendationResultDto>> result) {
        if (remaining.isEmpty()) {
            result.add(List.copyOf(prefix));
            return;
        }
        for (int i = 0; i < remaining.size(); i++) {
            RecommendationResultDto candidate = remaining.remove(i);
            prefix.add(candidate);
            permute(prefix, remaining, result);
            prefix.remove(prefix.size() - 1);
            remaining.add(i, candidate);
        }
    }

    private List<OrderedCandidate> ordered(double latitude, double longitude,
            List<RecommendationResultDto> items) {
        List<OrderedCandidate> result = new ArrayList<>();
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
            if (categories.role(order.get(i - 1).item()) == CourseCandidateSelector.PlaceRole.MEAL
                    && categories.role(order.get(i).item()) == CourseCandidateSelector.PlaceRole.MEAL) count++;
        }
        return count;
    }

    private int sameRoleAdjacencies(List<OrderedCandidate> order) {
        int count = 0;
        for (int i = 1; i < order.size(); i++) {
            if (categories.role(order.get(i - 1).item()) == categories.role(order.get(i).item())) count++;
        }
        return count;
    }

    private double totalDistance(List<OrderedCandidate> order) {
        return order.stream().mapToDouble(OrderedCandidate::distanceMeters).sum();
    }

    private String orderKey(List<OrderedCandidate> order) {
        return order.stream().map(item -> String.format("%020d", item.item().placeId()))
                .reduce("", String::concat);
    }

    private double distance(double fromLatitude, double fromLongitude, Double toLatitude, Double toLongitude) {
        if (toLatitude == null || toLongitude == null) return Double.MAX_VALUE;
        return DistanceCalculator.calculateKm(fromLatitude, fromLongitude, toLatitude, toLongitude) * 1_000;
    }

    public record OrderedCandidate(RecommendationResultDto item, double distanceMeters) { }
}
