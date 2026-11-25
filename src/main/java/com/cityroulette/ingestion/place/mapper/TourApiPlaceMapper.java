package com.cityroulette.ingestion.place.mapper;

import com.cityroulette.ingestion.place.dto.NormalizedPlace;
import com.cityroulette.integration.tourapi.dto.TourismPlaceDto;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;

/**
 * 외부 또는 상위 계층의 값을 내부에서 사용하는 형식으로 변환합니다.
 */
@Component
public class TourApiPlaceMapper implements ProviderPlaceMapper<TourismPlaceDto> {
    private static final String SOURCE_URL = "https://www.data.go.kr/data/15101578/openapi.do";
    private static final Map<String, String> CATEGORIES = Map.of(
            "12", "관광지", "14", "문화시설", "15", "행사/공연/축제", "25", "여행코스",
            "28", "레포츠", "32", "숙박", "38", "쇼핑", "39", "음식점");
    private final PlaceTextNormalizer text;
    private final PlaceCoordinateNormalizer coordinates;

    public TourApiPlaceMapper(PlaceTextNormalizer text, PlaceCoordinateNormalizer coordinates) {
        this.text = text; this.coordinates = coordinates;
    }

    @Override
    public NormalizedPlace map(TourismPlaceDto item) {
        String name = text.trimToNull(item.title());
        String address = text.combine(item.addr1(), item.addr2());
        PlaceCoordinateNormalizer.Coordinates point = coordinates.normalize(item.mapy(), item.mapx());
        return new NormalizedPlace(NormalizedPlace.RecordType.PLACE, "TOUR_API",
                text.trimToNull(item.contentid()), name, text.normalizeName(name),
                CATEGORIES.getOrDefault(item.contenttypeid(), "기타"), address,
                text.normalizeAddress(address), districtFrom(address), point.latitude(), point.longitude(),
                null, text.trimToNull(item.firstimage()), SOURCE_URL, sourceCategory(item),
                parseTime(item.modifiedtime()), "공공데이터포털 이용허락범위 제한 없음",
                text.trimToNull(item.cpyrhtDivCd()), "한국관광공사 TourAPI");
    }

    private String sourceCategory(TourismPlaceDto item) {
        return java.util.stream.Stream.of(item.contenttypeid(), item.lclsSystm1(), item.lclsSystm2(), item.lclsSystm3())
                .map(text::trimToNull).filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.joining("/"));
    }
    private String districtFrom(String address) {
        if (address == null) return null;
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("부산(?:광역시)?\\s+([가-힣]+(?:구|군))").matcher(address);
        return matcher.find() ? matcher.group(1) : null;
    }
    private LocalDateTime parseTime(String value) {
        value = text.trimToNull(value);
        if (value == null) return null;
        try { return LocalDateTime.parse(value, DateTimeFormatter.ofPattern("yyyyMMddHHmmss")); }
        catch (DateTimeParseException exception) { return null; }
    }
}
