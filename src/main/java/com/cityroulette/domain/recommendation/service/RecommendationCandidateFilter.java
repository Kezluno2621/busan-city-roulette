package com.cityroulette.domain.recommendation.service;

import com.cityroulette.domain.recommendation.config.RecommendationSettings;
import com.cityroulette.domain.place.entity.Place;
import com.cityroulette.domain.recommendation.dto.CategorySelectionMode;
import com.cityroulette.domain.recommendation.dto.RecommendationRequest;
import com.cityroulette.domain.place.repository.PlaceRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
public class RecommendationCandidateFilter {
    private static final Map<String, String> CATEGORY_ALIASES = Map.ofEntries(
            Map.entry("ATTRACTION", "관광지"), Map.entry("TOURIST_SPOT", "관광지"),
            Map.entry("CAFE", "카페"), Map.entry("RESTAURANT", "음식점"),
            Map.entry("FOOD", "음식점"), Map.entry("CULTURE", "문화시설"),
            Map.entry("CULTURAL_FACILITY", "문화시설"), Map.entry("SHOPPING", "쇼핑"),
            Map.entry("FESTIVAL", "행사"), Map.entry("EVENT", "행사"));

    private final PlaceRepository places;
    private final RecommendationSettings settings;

    public RecommendationCandidateFilter(PlaceRepository places, RecommendationSettings settings) {
        this.places = places;
        this.settings = settings;
    }

    /** DB 조회 상한을 적용하고 출발 장소와 이름이 중복된 후보를 제외합니다. */
    public CandidateSelection select(RecommendationRequest request, Long excludedPlaceId, int requestedLimit) {
        Set<String> preferred = normalizeCategories(request.categories());
        CategorySelectionMode mode = CategorySelectionMode.defaultIfNull(request.categoryMode());
        Set<String> queryCategorySet = new LinkedHashSet<>();
        preferred.stream().map(x -> x.toLowerCase(Locale.ROOT)).forEach(queryCategorySet::add);
        if (request.categories() != null) request.categories().stream()
                .filter(java.util.Objects::nonNull).map(String::trim).filter(x -> !x.isBlank())
                .map(x -> x.toLowerCase(Locale.ROOT)).forEach(queryCategorySet::add);
        List<String> queryCategories = queryCategorySet.isEmpty()
                ? List.of("__all_categories__") : List.copyOf(queryCategorySet);
        String district = blankToNull(request.district());
        int poolLimit = Math.min(settings.limits().candidatePool(), Math.max(50, requestedLimit * 10));
        List<Place> queried = places.findRecommendationCandidates(
                mode == CategorySelectionMode.ONLY && !preferred.isEmpty(),
                queryCategories, district, PageRequest.of(0, poolLimit));

        LinkedHashMap<String, Place> unique = new LinkedHashMap<>();
        for (Place place : queried) {
            if (excludedPlaceId != null && excludedPlaceId.equals(place.getId())) continue;
            unique.putIfAbsent(identity(place), place);
        }
        return new CandidateSelection(new ArrayList<>(unique.values()), preferred);
    }

    public String normalizeCategory(String value) {
        if (value == null) return "";
        String trimmed = value.trim();
        return CATEGORY_ALIASES.getOrDefault(trimmed.toUpperCase(Locale.ROOT), trimmed);
    }

    private Set<String> normalizeCategories(List<String> values) {
        Set<String> result = new LinkedHashSet<>();
        if (values == null) return result;
        values.stream().map(this::normalizeCategory).filter(x -> !x.isBlank()).forEach(result::add);
        return result;
    }

    private String identity(Place place) {
        String name = place.getNormalizedName();
        if (name == null || name.isBlank()) name = place.getName();
        return name == null ? "id:" + place.getId()
                : name.toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public record CandidateSelection(List<Place> places, Set<String> preferredCategories) { }
}
