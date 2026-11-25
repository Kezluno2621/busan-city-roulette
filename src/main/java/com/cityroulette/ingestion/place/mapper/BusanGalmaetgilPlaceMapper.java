package com.cityroulette.ingestion.place.mapper;

import com.cityroulette.integration.busan.dto.BusanGalmaetgilItem;
import com.cityroulette.ingestion.place.dto.NormalizedPlace;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 외부 또는 상위 계층의 값을 내부에서 사용하는 형식으로 변환합니다.
 */
@Component
public class BusanGalmaetgilPlaceMapper implements ProviderPlaceMapper<BusanGalmaetgilItem> {
    private static final String OPEN_LICENSE = "공공데이터포털 이용허락범위 제한 없음";
    private final PlaceTextNormalizer text; private final PlaceCoordinateNormalizer coordinates;
    public BusanGalmaetgilPlaceMapper(PlaceTextNormalizer text, PlaceCoordinateNormalizer coordinates) {
        this.text = text; this.coordinates = coordinates;
    }
    @Override public NormalizedPlace map(BusanGalmaetgilItem item) {
        String name = first(item.title(), item.place());
        var point = coordinates.normalize(item.lat(), item.lng());
        String sourceCategory = Stream.of(item.cate1_nm(), item.course(), item.gugan())
                .map(text::trimToNull).filter(Objects::nonNull).collect(Collectors.joining("/"));
        return new NormalizedPlace(NormalizedPlace.RecordType.PLACE, "BUSAN_GALMAETGIL",
                text.trimToNull(item.uc_seq()), name, text.normalizeName(name),
                category(item.cate1_nm()), null, null, null, point.latitude(), point.longitude(),
                text.trimToNull(item.itemcntnts()), image(item.main_img_n()),
                "https://www.data.go.kr/data/15077606/openapi.do", sourceCategory, null,
                OPEN_LICENSE, OPEN_LICENSE, "부산광역시 갈맷길 관광정보");
    }
    private String first(String one, String two) {
        String value = text.trimToNull(one); return value == null ? text.trimToNull(two) : value;
    }
    private String category(String value) {
        value = text.trimToNull(value);
        if (value == null) return "기타";
        if (value.contains("맛집")) return "음식점";
        if (value.contains("숙박")) return "숙박";
        return "관광지";
    }
    private String image(String value) {
        value = text.trimToNull(value);
        if (value == null || value.startsWith("http://") || value.startsWith("https://")) return value;
        return "https://www.visitbusan.net/" + (value.startsWith("/") ? value.substring(1) : value);
    }
}
