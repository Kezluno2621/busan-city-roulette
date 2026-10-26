package com.cityroulette.controller;

import com.cityroulette.dto.location.LocationSearchResultDto;
import com.cityroulette.dto.recommendation.RecommendationResponse;
import com.cityroulette.service.KakaoLocalApiException;
import com.cityroulette.service.LocationSearchService;
import com.cityroulette.service.RecommendationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class RecommendationPageController {

    private static final int DEFAULT_AVAILABLE_MINUTES = 180;
    private static final int DEFAULT_STAY_MINUTES = 60;
    private static final int DEFAULT_LIMIT = 1;

    private final RecommendationService recommendationService;
    private final LocationSearchService locationSearchService;

    public RecommendationPageController(
            RecommendationService recommendationService,
            LocationSearchService locationSearchService) {
        this.recommendationService = recommendationService;
        this.locationSearchService = locationSearchService;
    }

    @GetMapping("/recommend")
    public String showRecommendationForm(Model model) {
        prepareModel(model, "", "", "", "",
                DEFAULT_AVAILABLE_MINUTES, DEFAULT_STAY_MINUTES, DEFAULT_LIMIT);
        return "recommend";
    }

    @PostMapping("/recommend/search-location")
    public String searchLocation(
            @RequestParam(name = "locationQuery", defaultValue = "") String locationQuery,
            @RequestParam(name = "departureName", defaultValue = "") String departureName,
            @RequestParam(name = "latitude", defaultValue = "") String latitude,
            @RequestParam(name = "longitude", defaultValue = "") String longitude,
            @RequestParam(name = "availableMinutes", defaultValue = "180") String availableMinutes,
            @RequestParam(name = "stayMinutes", defaultValue = "60") String stayMinutes,
            @RequestParam(name = "limit", defaultValue = "1") String limit,
            Model model) {
        prepareModel(model, locationQuery, departureName, latitude, longitude,
                availableMinutes, stayMinutes, limit);

        String trimmedQuery = locationQuery.trim();
        if (trimmedQuery.isEmpty()) {
            model.addAttribute("locationSearchError", "검색할 출발지를 입력해주세요.");
            return "recommend";
        }
        if (trimmedQuery.length() > 100) {
            model.addAttribute("locationSearchError", "출발지 검색어는 100자 이내로 입력해주세요.");
            return "recommend";
        }

        try {
            List<LocationSearchResultDto> results = locationSearchService.search(trimmedQuery);
            model.addAttribute("locationSearchResults", results);
            if (results.isEmpty()) {
                model.addAttribute("locationSearchError", "부산에서 일치하는 출발지를 찾지 못했습니다.");
            }
        } catch (KakaoLocalApiException exception) {
            model.addAttribute("locationSearchError",
                    "출발지 검색을 사용할 수 없습니다. KAKAO_REST_API_KEY 설정을 확인해주세요.");
        }
        return "recommend";
    }

    @PostMapping("/recommend")
    public String recommend(
            @RequestParam(name = "locationQuery", defaultValue = "") String locationQuery,
            @RequestParam(name = "departureName", defaultValue = "") String departureName,
            @RequestParam(name = "latitude", defaultValue = "") String latitudeInput,
            @RequestParam(name = "longitude", defaultValue = "") String longitudeInput,
            @RequestParam(name = "availableMinutes") String availableMinutesInput,
            @RequestParam(name = "stayMinutes") String stayMinutesInput,
            @RequestParam(name = "limit") String limitInput,
            Model model) {
        Integer availableMinutes = parseInteger(availableMinutesInput);
        Integer stayMinutes = parseInteger(stayMinutesInput);
        Integer limit = parseInteger(limitInput);
        prepareModel(model, locationQuery, departureName, latitudeInput, longitudeInput,
                availableMinutesInput, stayMinutesInput, limitInput);

        if (departureName.isBlank() || latitudeInput.isBlank() || longitudeInput.isBlank()) {
            model.addAttribute("inputError", "출발지를 검색한 뒤 결과에서 한 곳을 선택해주세요.");
            return "recommend";
        }

        Double latitude = parseDouble(latitudeInput);
        Double longitude = parseDouble(longitudeInput);
        String validationMessage = validate(
                latitude, longitude, availableMinutes, stayMinutes, limit);
        if (validationMessage != null) {
            model.addAttribute("inputError", validationMessage);
            return "recommend";
        }

        RecommendationResponse response = recommendationService.recommend(
                latitude, longitude, availableMinutes, stayMinutes, limit);
        model.addAttribute("recommendationResponse", response);
        return "recommend";
    }

    private String validate(
            Double latitude,
            Double longitude,
            Integer availableMinutes,
            Integer stayMinutes,
            Integer limit) {
        if (latitude == null || !Double.isFinite(latitude)
                || latitude < -90 || latitude > 90) {
            return "선택한 출발지의 위도 정보가 올바르지 않습니다. 다시 검색해주세요.";
        }
        if (longitude == null || !Double.isFinite(longitude)
                || longitude < -180 || longitude > 180) {
            return "선택한 출발지의 경도 정보가 올바르지 않습니다. 다시 검색해주세요.";
        }
        if (availableMinutes == null || availableMinutes < 60 || availableMinutes > 720) {
            return "사용 가능 시간을 확인해주세요.";
        }
        if (stayMinutes == null || stayMinutes < 30 || stayMinutes > 240) {
            return "예상 체류시간을 확인해주세요.";
        }
        if (limit == null || limit < 1 || limit > 3) {
            return "후보 탐색 수는 1개에서 3개 사이로 선택해주세요.";
        }
        return null;
    }

    private Double parseDouble(String value) {
        try {
            return Double.valueOf(value.trim());
        } catch (NullPointerException | NumberFormatException exception) {
            return null;
        }
    }

    private Integer parseInteger(String value) {
        try {
            return Integer.valueOf(value.trim());
        } catch (NullPointerException | NumberFormatException exception) {
            return null;
        }
    }

    private void prepareModel(
            Model model,
            Object locationQuery,
            Object departureName,
            Object latitude,
            Object longitude,
            Object availableMinutes,
            Object stayMinutes,
            Object limit) {
        model.addAttribute("title", "City Roulette 추천");
        model.addAttribute("locationQuery", locationQuery);
        model.addAttribute("departureName", departureName);
        model.addAttribute("latitude", latitude);
        model.addAttribute("longitude", longitude);
        model.addAttribute("availableMinutes", availableMinutes);
        model.addAttribute("stayMinutes", stayMinutes);
        model.addAttribute("limit", limit);
    }
}
