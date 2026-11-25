package com.cityroulette.ingestion.place.mapper;

import com.cityroulette.integration.busan.dto.BusanAttractionItem;
import com.cityroulette.ingestion.place.dto.NormalizedPlace;
import org.springframework.stereotype.Component;

/**
 * 외부 또는 상위 계층의 값을 내부에서 사용하는 형식으로 변환합니다.
 */
@Component
public class BusanAttractionPlaceMapper implements ProviderPlaceMapper<BusanAttractionItem> {
    private static final String OPEN_LICENSE = "공공데이터포털 이용허락범위 제한 없음";
    private final PlaceTextNormalizer text; private final PlaceCoordinateNormalizer coordinates;
    public BusanAttractionPlaceMapper(PlaceTextNormalizer text, PlaceCoordinateNormalizer coordinates) {
        this.text = text; this.coordinates = coordinates;
    }
    @Override public NormalizedPlace map(BusanAttractionItem item) {
        String name = first(item.mainTitle(), item.title());
        String address = text.trimToNull(item.address());
        var point = coordinates.normalize(item.latitude(), item.longitude());
        return new NormalizedPlace(NormalizedPlace.RecordType.PLACE, "BUSAN_ATTRACTION",
                text.trimToNull(item.externalId()), name, text.normalizeName(name), "관광지",
                address, text.normalizeAddress(address), text.trimToNull(item.district()),
                point.latitude(), point.longitude(), text.trimToNull(item.description()),
                text.trimToNull(item.imageUrl()), "https://www.data.go.kr/data/15063481/openapi.do",
                "ATTRACTION", null, OPEN_LICENSE, OPEN_LICENSE, "부산광역시 부산명소정보");
    }
    private String first(String one, String two) {
        String value = text.trimToNull(one); return value == null ? text.trimToNull(two) : value;
    }
}
