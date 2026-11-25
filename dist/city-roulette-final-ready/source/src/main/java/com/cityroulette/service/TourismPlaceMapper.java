package com.cityroulette.service;

import com.cityroulette.domain.Place;
import com.cityroulette.dto.tourism.TourismPlaceDto;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Component
public class TourismPlaceMapper {

    private static final double BUSAN_MIN_LATITUDE = 34.80;
    private static final double BUSAN_MAX_LATITUDE = 35.40;
    private static final double BUSAN_MIN_LONGITUDE = 128.70;
    private static final double BUSAN_MAX_LONGITUDE = 129.35;
    private static final DateTimeFormatter TOUR_API_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private static final Pattern BUSAN_DISTRICT_PATTERN = Pattern.compile(
            "부산(?:광역시)?\\s+([가-힣]+(?:구|군))");

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
        String address = combineAddress(dto.addr1(), dto.addr2());
        Place place = new Place(
                Place.SOURCE_KOR_SERVICE,
                trimToNull(dto.contentid()),
                trimToNull(dto.title()),
                categoryOf(dto.contenttypeid()),
                address,
                districtOf(address),
                latitudeOf(dto.mapy(), dto.mapx()),
                longitudeOf(dto.mapy(), dto.mapx()),
                null,
                null
        );
        place.updateFromApi(place.getName(), place.getCategory(), place.getAddress(),
                place.getDistrict(), place.getLatitude(), place.getLongitude(), null,
                trimToNull(dto.firstimage()));
        return place;
    }

    public boolean isBusan(TourismPlaceDto dto) {
        return dto != null && "26".equals(trimToNull(dto.lDongRegnCd()));
    }

    public String sourceCategoryOf(TourismPlaceDto dto) {
        if (dto == null) return null;
        return String.join("/", java.util.stream.Stream.of(dto.contenttypeid(), dto.lclsSystm1(),
                        dto.lclsSystm2(), dto.lclsSystm3()).map(this::trimToNull)
                .filter(java.util.Objects::nonNull).toList());
    }

    public LocalDateTime sourceModifiedAtOf(TourismPlaceDto dto) {
        String value = dto == null ? null : trimToNull(dto.modifiedtime());
        if (value == null) return null;
        try { return LocalDateTime.parse(value, TOUR_API_TIME); }
        catch (DateTimeParseException exception) { return null; }
    }

    public String imageLicenseOf(TourismPlaceDto dto) {
        return dto == null ? null : trimToNull(dto.cpyrhtDivCd());
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

    private String districtOf(String address) {
        if (address == null) {
            return null;
        }
        Matcher matcher = BUSAN_DISTRICT_PATTERN.matcher(address);
        return matcher.find() ? matcher.group(1) : null;
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

    private Double latitudeOf(String latitude, String longitude) {
        Double lat = parseCoordinate(latitude);
        Double lon = parseCoordinate(longitude);
        return isValidBusanCoordinate(lat, lon) ? lat : null;
    }

    private Double longitudeOf(String latitude, String longitude) {
        Double lat = parseCoordinate(latitude);
        Double lon = parseCoordinate(longitude);
        return isValidBusanCoordinate(lat, lon) ? lon : null;
    }

    public boolean isValidBusanCoordinate(Double latitude, Double longitude) {
        if (latitude == null || longitude == null || !Double.isFinite(latitude) || !Double.isFinite(longitude)) {
            return false;
        }
        return latitude >= BUSAN_MIN_LATITUDE && latitude <= BUSAN_MAX_LATITUDE
                && longitude >= BUSAN_MIN_LONGITUDE && longitude <= BUSAN_MAX_LONGITUDE;
    }
}
