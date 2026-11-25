package com.cityroulette.controller;

import com.cityroulette.dto.location.LocationSearchResultDto;
import com.cityroulette.dto.recommendation.RecommendationResponse;
import com.cityroulette.service.KakaoLocalApiException;
import com.cityroulette.service.LocationSearchService;
import com.cityroulette.service.OdsayApiException;
import com.cityroulette.service.RecommendationService;
import com.cityroulette.service.RecommendationInputValidator;
import com.cityroulette.service.RecommendationInputValidator.ValidationError;
import com.cityroulette.config.RecommendationSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 화면 요청을 받고 Service의 결과와 함께 Thymeleaf에 전달할 뷰를 선택합니다.
 */
@Controller
public class RecommendationPageController {

    private static final Logger log = LoggerFactory.getLogger(RecommendationPageController.class);
    private final RecommendationService recommendationService;
    private final LocationSearchService locationSearchService;
    private final RecommendationInputValidator validator;
    private final RecommendationSettings settings;

    public RecommendationPageController(
            RecommendationService recommendationService,
            LocationSearchService locationSearchService,
            RecommendationInputValidator validator,
            RecommendationSettings settings) {
        this.recommendationService = recommendationService;
        this.locationSearchService = locationSearchService;
        this.validator = validator;
        this.settings = settings;
    }

    @GetMapping("/recommend")
    public String showRecommendationForm(Model model) {
        prepareModel(model, "", "", "", "",
                settings.defaults().availableMinutes(), settings.defaults().stayMinutes(),
                settings.defaults().uiCandidateLimit());
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
            // 키나 전체 요청 URL은 남기지 않고 상태 코드만 기록한다.
            log.warn("Kakao Local API search failed: status={}", exception.getHttpStatusCode());
            model.addAttribute("locationSearchError", kakaoSearchErrorMessage(exception));
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
        String validationMessage = validationMessage(validator.validate(
                latitude, longitude, availableMinutes, stayMinutes, limit, true));
        if (validationMessage != null) {
            model.addAttribute("inputError", validationMessage);
            return "recommend";
        }

        try {
            RecommendationResponse response = recommendationService.recommend(
                    latitude, longitude, availableMinutes, stayMinutes, limit);
            model.addAttribute("recommendationResponse", response);
        } catch (OdsayApiException exception) {
            model.addAttribute("inputError",
                    "대중교통 경로 조회를 사용할 수 없습니다. 잠시 후 다시 시도해주세요.");
        }
        return "recommend";
    }

    private String validationMessage(ValidationError error) {
        if (error == null) return null;
        return switch (error) {
            case LATITUDE -> "선택한 출발지의 위도 정보가 올바르지 않습니다. 다시 검색해주세요.";
            case LONGITUDE -> "선택한 출발지의 경도 정보가 올바르지 않습니다. 다시 검색해주세요.";
            case AVAILABLE_MINUTES -> "사용 가능 시간을 확인해주세요.";
            case STAY_MINUTES -> "예상 체류시간을 확인해주세요.";
            case CANDIDATE_LIMIT -> "후보 탐색 수는 1개에서 3개 사이로 선택해주세요.";
        };
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

    private String kakaoSearchErrorMessage(KakaoLocalApiException exception) {
        Integer statusCode = exception.getHttpStatusCode();
        if (statusCode == null) {
            return "카카오 장소 검색 서버에 연결할 수 없습니다. 인터넷 연결과 STS 실행 환경을 확인해주세요.";
        }
        return switch (statusCode) {
            case 401 -> "카카오 REST API 키 인증에 실패했습니다. STS의 KAKAO_REST_API_KEY가 앱의 REST API 키인지 확인해주세요.";
            case 403 -> "카카오 Local API 사용 권한이 거부되었습니다. 카카오 개발자 콘솔의 앱과 권한 설정을 확인해주세요.";
            case 429 -> "카카오 장소 검색 호출 한도를 초과했습니다. 잠시 후 다시 시도해주세요.";
            default -> "카카오 장소 검색이 일시적으로 실패했습니다. 잠시 후 다시 시도해주세요. (HTTP " + statusCode + ")";
        };
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
