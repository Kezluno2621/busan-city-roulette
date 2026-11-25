package com.cityroulette.domain.place.service;

import com.cityroulette.domain.place.entity.Place;
import com.cityroulette.domain.place.entity.PlaceSource;
import com.cityroulette.domain.place.repository.PlaceRepository;
import com.cityroulette.domain.place.repository.PlaceSourceRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class PlaceImagePolicyServiceTest {
    @Autowired PlaceImagePolicyService images;
    @Autowired PlaceRepository places;
    @Autowired PlaceSourceRepository sources;

    @BeforeEach void clean() {
        sources.deleteAll();
        places.deleteAll();
    }

    @Test void allowsLicensedPublicImageAndBlocksUnknownLicense() {
        Place allowed = place("허가 이미지", "https://images.example.org/place.jpg");
        source(allowed, "공공누리 제1유형");
        Place unknown = place("미확인 이미지", "https://images.example.org/unknown.jpg");
        source(unknown, null);

        assertThat(images.displayablePlaceIds(java.util.List.of(allowed.getId(), unknown.getId())))
                .containsExactly(allowed.getId());
    }

    @Test void blocksInternalAndMalformedImageUrlsEvenWithLicense() {
        Place internal = place("내부 주소", "http://127.0.0.1/private.jpg");
        source(internal, "공공누리 제1유형");
        Place malformed = place("잘못된 주소", "javascript:alert(1)");
        source(malformed, "CC BY 4.0");
        Place localIpv6 = place("IPv6 내부 주소", "http://[::1]/private.jpg");
        source(localIpv6, "CC BY 4.0");

        assertThat(images.displayablePlaceIds(java.util.List.of(
                internal.getId(), malformed.getId(), localIpv6.getId()))).isEmpty();
    }

    @Test void missingImageUsesFallbackByBeingExcluded() {
        Place missing = place("이미지 없음", null);
        source(missing, "공공누리 제1유형");

        assertThat(images.canDisplay(missing.getId())).isFalse();
    }

    private Place place(String name, String imageUrl) {
        Place place = new Place(name, "관광지", "부산", 35.1, 129.1, false);
        place.updateFromApi(name, "관광지", "부산", "해운대구", 35.1, 129.1, "설명", imageUrl);
        return places.save(place);
    }

    private void source(Place place, String imageLicense) {
        PlaceSource source = new PlaceSource(place, "TEST", "source-" + place.getId());
        source.synchronize("https://example.org/source", "관광지", null, LocalDateTime.now(),
                "테스트 제공자", "공공데이터", imageLicense);
        sources.save(source);
    }
}
