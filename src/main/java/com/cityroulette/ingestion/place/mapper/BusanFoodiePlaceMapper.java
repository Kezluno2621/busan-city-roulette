package com.cityroulette.ingestion.place.mapper;

import com.cityroulette.integration.busan.dto.BusanFoodieItem;
import com.cityroulette.ingestion.place.dto.NormalizedPlace;
import org.springframework.stereotype.Component;

/**
 * 외부 또는 상위 계층의 값을 내부에서 사용하는 형식으로 변환합니다.
 */
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
