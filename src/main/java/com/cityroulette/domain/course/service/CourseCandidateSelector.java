package com.cityroulette.domain.course.service;

import com.cityroulette.domain.recommendation.config.RecommendationSettings;
import com.cityroulette.domain.recommendation.dto.CategorySelectionMode;
import com.cityroulette.domain.recommendation.dto.RecommendationResultDto;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;

/** 추천 후보의 중복 제거, 카테고리 다양성, 가중 랜덤 선택만 담당합니다. */
@Component
public class CourseCandidateSelector {
    private final RecommendationSettings settings;

    public CourseCandidateSelector(RecommendationSettings settings) {
        this.settings = settings;
    }

    /** 유효한 후보에서 역할 다양성을 고려해 중복 없이 방문 장소를 고릅니다. */
    public List<RecommendationResultDto> select(List<RecommendationResultDto> candidates,
            int requestedCount, int randomness, Long requestedSeed,
            CategorySelectionMode categoryMode, List<String> requestedCategories) {
        LinkedHashMap<Long, RecommendationResultDto> unique = new LinkedHashMap<>();
        candidates.stream().filter(this::hasValidCoordinates)
                .forEach(candidate -> unique.putIfAbsent(candidate.placeId(), candidate));
        List<RecommendationResultDto> available = new ArrayList<>(unique.values());
        int count = Math.min(requestedCount, available.size());
        if (count == 0) return List.of();

        List<RecommendationResultDto> selected = new ArrayList<>();
        Set<PlaceRole> selectedRoles = new LinkedHashSet<>();
        long seed = requestedSeed == null ? ThreadLocalRandom.current().nextLong() : requestedSeed;
        SplittableRandom random = new SplittableRandom(seed);

        if ((categoryMode == CategorySelectionMode.REQUIRED || categoryMode == CategorySelectionMode.PREFERRED)
                && requestedCategories != null) {
            addRequestedCategories(available, selected, selectedRoles, requestedCategories, count);
        }

        while (selected.size() < count && !available.isEmpty()) {
            List<RecommendationResultDto> diverse = available.stream()
                    .filter(candidate -> role(candidate) != PlaceRole.OTHER)
                    .filter(candidate -> !selectedRoles.contains(role(candidate)))
                    .toList();
            List<RecommendationResultDto> choicePool = categoryMode == CategorySelectionMode.ONLY || diverse.isEmpty()
                    ? available : diverse;
            RecommendationResultDto chosen = randomness == 0
                    ? deterministicChoice(choicePool, selectedRoles)
                    : randomChoice(choicePool, selectedRoles, count - selected.size(), randomness, random);
            selected.add(chosen);
            selectedRoles.add(role(chosen));
            available.remove(chosen);
        }
        return selected;
    }

    private void addRequestedCategories(List<RecommendationResultDto> available,
            List<RecommendationResultDto> selected, Set<PlaceRole> selectedRoles,
            List<String> requestedCategories, int count) {
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

    private RecommendationResultDto deterministicChoice(List<RecommendationResultDto> candidates,
            Set<PlaceRole> selectedRoles) {
        return candidates.stream().max(Comparator
                .comparingDouble((RecommendationResultDto item) -> item.score()
                        + (selectedRoles.contains(role(item)) ? 0
                        : settings.course().categoryNoveltyBonus()))
                .thenComparingDouble(item -> -item.distanceMeters())
                .thenComparing(item -> -item.placeId())).orElseThrow();
    }

    private RecommendationResultDto randomChoice(List<RecommendationResultDto> candidates,
            Set<PlaceRole> selectedRoles, int remaining, int randomness, SplittableRandom random) {
        int poolSize = Math.min(candidates.size(), remaining
                + (int) Math.ceil((candidates.size() - remaining) * randomness / 100.0));
        List<RecommendationResultDto> pool = candidates.subList(0, poolSize);
        double exponent = 4.0 - 3.3 * randomness / 100.0;
        // 각 장소가 차지하는 가중치 구간을 이어 붙인 뒤 난수가 속한 구간의 장소를 고릅니다.
        double total = pool.stream().mapToDouble(item -> randomWeight(item, selectedRoles, exponent)).sum();
        double pick = random.nextDouble(total);
        RecommendationResultDto chosen = pool.get(pool.size() - 1);
        for (RecommendationResultDto item : pool) {
            pick -= randomWeight(item, selectedRoles, exponent);
            if (pick <= 0) return item;
        }
        return chosen;
    }

    private double randomWeight(RecommendationResultDto item, Set<PlaceRole> selectedRoles, double exponent) {
        double diversity = selectedRoles.contains(role(item)) ? 1.0 : 1.35;
        return Math.pow(Math.max(1, item.score() + 1), exponent) * diversity;
    }

    public List<String> missingDiversityTypes(List<RecommendationResultDto> candidates) {
        Set<PlaceRole> roles = new LinkedHashSet<>();
        candidates.forEach(candidate -> roles.add(role(candidate)));
        List<String> missing = new ArrayList<>();
        if (!roles.contains(PlaceRole.SIGHTSEEING)) missing.add("관광지");
        if (!roles.contains(PlaceRole.CAFE)) missing.add("카페");
        if (!roles.contains(PlaceRole.MEAL)) missing.add("음식점");
        if (!roles.contains(PlaceRole.CULTURE)) missing.add("문화시설");
        return missing;
    }

    public PlaceRole role(RecommendationResultDto item) {
        return role(item.category());
    }

    public PlaceRole role(String category) {
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

    public enum PlaceRole { MEAL, CAFE, SIGHTSEEING, CULTURE, WALKING, OTHER }
}
