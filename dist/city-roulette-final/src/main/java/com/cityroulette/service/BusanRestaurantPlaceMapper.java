package com.cityroulette.service;

import com.cityroulette.dto.importing.BusanRestaurantItem;
import com.cityroulette.dto.importing.NormalizedPlace;
import org.springframework.stereotype.Component;

@Component
public class BusanRestaurantPlaceMapper implements ProviderPlaceMapper<BusanRestaurantItem> {
    private static final String OPEN_LICENSE = "공공데이터포털 이용허락범위 제한 없음";
    private final PlaceTextNormalizer text; private final PlaceCoordinateNormalizer coordinates;
    public BusanRestaurantPlaceMapper(PlaceTextNormalizer text, PlaceCoordinateNormalizer coordinates) {
        this.text = text; this.coordinates = coordinates;
    }
    @Override public NormalizedPlace map(BusanRestaurantItem item) {
        String name = first(item.mainTitle(), item.title());
        String address = text.combine(item.address1(), item.address2());
        var point = coordinates.normalize(item.latitude(), item.longitude());
        return new NormalizedPlace(NormalizedPlace.RecordType.PLACE, "BUSAN_RESTAURANT",
                text.trimToNull(item.externalId()), name, text.normalizeName(name), "음식점",
                address, text.normalizeAddress(address), text.trimToNull(item.district()),
                point.latitude(), point.longitude(), text.trimToNull(item.description()),
                text.trimToNull(item.imageUrl()), "https://www.data.go.kr/data/15063472/openapi.do",
                "FOOD", null, OPEN_LICENSE, OPEN_LICENSE, "부산광역시 부산맛집정보");
    }
    private String first(String one, String two) {
        String value = text.trimToNull(one); return value == null ? text.trimToNull(two) : value;
    }
}
