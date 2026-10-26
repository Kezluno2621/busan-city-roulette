package com.cityroulette.service;

import com.cityroulette.domain.Place;
import com.cityroulette.dto.tourism.TourismPlaceDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class TourismPlaceMapperTest {

    private final TourismPlaceMapper mapper = new TourismPlaceMapper();

    @Test
    void mapsTourismPlaceToPlace() {
        Place place = mapper.toPlace(dto("12", "부산광역시 영도구 동삼동", "632-3",
                "129.0643000000", "35.0692000000"));

        assertThat(place.getExternalId()).isEqualTo("3017435");
        assertThat(place.getName()).isEqualTo("중리노을전망대");
        assertThat(place.getAddress()).isEqualTo("부산광역시 영도구 동삼동 632-3");
        assertThat(place.getLatitude()).isEqualTo(35.0692);
        assertThat(place.getLongitude()).isEqualTo(129.0643);
        assertThat(place.getCategory()).isEqualTo("관광지");
        assertThat(place.getIndoor()).isNull();
    }

    @Test
    void omitsBlankAddressPartAndInvalidCoordinates() {
        Place place = mapper.toPlace(dto("unknown", " 부산광역시 사하구 ", " ",
                "not-a-number", ""));

        assertThat(place.getAddress()).isEqualTo("부산광역시 사하구");
        assertThat(place.getLongitude()).isNull();
        assertThat(place.getLatitude()).isNull();
        assertThat(place.getCategory()).isEqualTo("기타");
    }

    @Test
    void usesSecondAddressWhenFirstAddressIsMissing() {
        Place place = mapper.toPlace(dto("12", null, "해변 산책로", null, "   "));

        assertThat(place.getAddress()).isEqualTo("해변 산책로");
        assertThat(place.getLongitude()).isNull();
        assertThat(place.getLatitude()).isNull();
    }

    @Test
    void safelyMapsMissingCategoryAndAddress() {
        Place place = mapper.toPlace(dto(null, null, "", null, null));

        assertThat(place.getCategory()).isEqualTo("기타");
        assertThat(place.getAddress()).isNull();
    }

    @ParameterizedTest
    @CsvSource({
            "12, 관광지",
            "14, 문화시설",
            "15, 행사/공연/축제",
            "25, 여행코스",
            "28, 레포츠",
            "32, 숙박",
            "38, 쇼핑",
            "39, 음식점"
    })
    void mapsContentTypeToReadableCategory(String contentTypeId, String expectedCategory) {
        Place place = mapper.toPlace(dto(contentTypeId, null, null, null, null));

        assertThat(place.getCategory()).isEqualTo(expectedCategory);
    }

    private TourismPlaceDto dto(String contentTypeId, String addr1, String addr2,
                                String mapx, String mapy) {
        return new TourismPlaceDto(
                "3017435", contentTypeId, "중리노을전망대", addr1, addr2, mapx, mapy,
                "image", "thumbnail", "Type3", "26", "200",
                "VE", "VE01", "VE010100", "", "49113",
                "20230915120000", "20260915120000"
        );
    }
}
