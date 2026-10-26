package com.cityroulette.service;

import com.cityroulette.domain.Place;
import com.cityroulette.dto.tourism.TourismPlaceDto;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class TourismPlaceMapper {

    private static final Map<String, String> CATEGORIES = Map.of(
            "12", "관광지",
            "14", "문화시설",
            "15", "행사/공연/축제",
            "25", "여행코스",
            "28", "레포츠",
            "32", "숙박",
            "38", "쇼핑",
            "39", "음식점"
    );

    public Place toPlace(TourismPlaceDto dto) {
        return new Place(
                trimToNull(dto.contentid()),
                trimToNull(dto.title()),
                categoryOf(dto.contenttypeid()),
                combineAddress(dto.addr1(), dto.addr2()),
                parseCoordinate(dto.mapy()),
                parseCoordinate(dto.mapx()),
                null
        );
    }

    private String categoryOf(String contentTypeId) {
        if (contentTypeId == null) {
            return "기타";
        }
        return CATEGORIES.getOrDefault(contentTypeId, "기타");
    }

    private String combineAddress(String addr1, String addr2) {
        String first = trimToNull(addr1);
        String second = trimToNull(addr2);

        if (first == null) {
            return second;
        }
        if (second == null) {
            return first;
        }
        return first + " " + second;
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private Double parseCoordinate(String value) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            return null;
        }

        try {
            return Double.valueOf(trimmed);
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
