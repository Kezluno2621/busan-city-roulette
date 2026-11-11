package com.cityroulette.service;

import com.cityroulette.dto.importing.*;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProviderPlaceMapperTest {
    private final PlaceTextNormalizer text=new PlaceTextNormalizer();
    private final PlaceCoordinateNormalizer coordinates=new PlaceCoordinateNormalizer();

    @Test void mapsAttractionFromConfirmedFields() {
        var raw=new BusanAttractionItem("10"," 감천 문화마을 ","부제","부산광역시 사하구 감내2로","사하구",
                "35.097","129.010","설명","https://image","051");
        var mapped=new BusanAttractionPlaceMapper(text,coordinates).map(raw);
        assertThat(mapped.externalId()).isEqualTo("10");
        assertThat(mapped.name()).isEqualTo("감천 문화마을");
        assertThat(mapped.category()).isEqualTo("관광지");
        assertThat(mapped.latitude()).isEqualTo(35.097);
        assertThat(mapped.imageLicense()).isEqualTo("공공데이터포털 이용허락범위 제한 없음");
    }

    @Test void mapsRestaurantAndRejectsCoordinatesOutsideBusan() {
        var raw=new BusanRestaurantItem("20","맛집","","부산광역시 중구","중앙동","중구","37.5","127.0",
                "설명","img","tel","메뉴");
        var mapped=new BusanRestaurantPlaceMapper(text,coordinates).map(raw);
        assertThat(mapped.category()).isEqualTo("음식점");
        assertThat(mapped.address()).contains("중앙동");
        assertThat(mapped.latitude()).isNull();
        assertThat(mapped.longitude()).isNull();
        assertThat(mapped.imageLicense()).isEqualTo("공공데이터포털 이용허락범위 제한 없음");
    }

    @Test void classifiesFoodieThemeAndKeepsGalmaetgilPlaceSeparate() {
        var foodie=new BusanFoodiePlaceMapper(text).map(new BusanFoodieItem("30","미식 여행","","","","내용","img"));
        var galmaet=new BusanGalmaetgilPlaceMapper(text,coordinates).map(
                new BusanGalmaetgilItem("40","","전망대","장소","명소","35.1","129.1","1코스","1구간","/a.jpg","내용"));
        assertThat(foodie.recordType()).isEqualTo(NormalizedPlace.RecordType.THEME_CONTENT);
        assertThat(galmaet.recordType()).isEqualTo(NormalizedPlace.RecordType.PLACE);
        assertThat(galmaet.sourceCategory()).contains("1코스","1구간");
        assertThat(galmaet.imageLicense()).isEqualTo("공공데이터포털 이용허락범위 제한 없음");
    }

    @Test void normalizesNamesAndAddresses() {
        assertThat(text.normalizeName("  감천-문화마을 ")).isEqualTo("감천문화마을");
        assertThat(text.normalizeAddress("부산광역시  해운대구 해운대로 1")).isEqualTo("부산해운대구해운대로1");
    }
}
