package com.cityroulette.service;

import com.cityroulette.dto.importing.BusanFoodieItem;
import com.cityroulette.dto.importing.NormalizedPlace;
import org.springframework.stereotype.Component;

@Component
public class BusanFoodiePlaceMapper implements ProviderPlaceMapper<BusanFoodieItem> {
    private static final String OPEN_LICENSE = "공공데이터포털 이용허락범위 제한 없음";
    private final PlaceTextNormalizer text;
    public BusanFoodiePlaceMapper(PlaceTextNormalizer text) { this.text = text; }
    @Override public NormalizedPlace map(BusanFoodieItem item) {
        String name = text.trimToNull(item.mainTitle());
        return new NormalizedPlace(NormalizedPlace.RecordType.THEME_CONTENT, "BUSAN_FOODIE",
                text.trimToNull(item.externalId()), name, text.normalizeName(name), "미식테마",
                null, null, null, null, null, text.trimToNull(item.description()),
                text.trimToNull(item.imageUrl()), "https://www.data.go.kr/data/15063507/openapi.do",
                "FOOD_THEME", null, OPEN_LICENSE, OPEN_LICENSE,
                "부산광역시 부산푸디투어");
    }
}
