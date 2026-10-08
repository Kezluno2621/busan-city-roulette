package com.cityroulette.repository;

import com.cityroulette.domain.Place;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PlaceRepositoryTests {

    @Autowired
    private PlaceRepository placeRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void savesAndFindsPlace() {
        Place place = new Place("테스트 전시관", "문화", "부산광역시 테스트 주소",
                35.1796, 129.0756, true);

        Place savedPlace = placeRepository.saveAndFlush(place);
        Long placeId = savedPlace.getId();
        assertThat(placeId).isNotNull();

        // 영속성 컨텍스트를 비워 메모리가 아닌 DB에서 다시 조회합니다.
        entityManager.clear();
        Place foundPlace = placeRepository.findById(placeId).orElseThrow();

        assertThat(foundPlace.getId()).isEqualTo(placeId);
        assertThat(foundPlace.getExternalId()).isNull();
        assertThat(foundPlace.getName()).isEqualTo("테스트 전시관");
        assertThat(foundPlace.getCategory()).isEqualTo("문화");
        assertThat(foundPlace.getAddress()).isEqualTo("부산광역시 테스트 주소");
        assertThat(foundPlace.getLatitude()).isEqualTo(35.1796);
        assertThat(foundPlace.getLongitude()).isEqualTo(129.0756);
        assertThat(foundPlace.getIndoor()).isTrue();
    }

    @Test
    void savesAndFindsExternalId() {
        Place place = new Place("00123456", "테스트 관광지", "관광지", "부산광역시 테스트 주소",
                35.1796, 129.0756, null);

        Long placeId = placeRepository.saveAndFlush(place).getId();
        entityManager.clear();
        Place foundPlace = placeRepository.findById(placeId).orElseThrow();

        assertThat(foundPlace.getExternalId()).isEqualTo("00123456");
        assertThat(placeRepository.existsByExternalId("00123456")).isTrue();
        assertThat(placeRepository.existsByExternalId("missing-id")).isFalse();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @NullSource
    void savesAndFindsIndoor(Boolean indoor) {
        Place place = new Place("테스트 장소", "관광지", "부산광역시 테스트 주소",
                35.1796, 129.0756, indoor);

        Long placeId = placeRepository.saveAndFlush(place).getId();
        entityManager.clear();
        Place foundPlace = placeRepository.findById(placeId).orElseThrow();

        assertThat(foundPlace.getIndoor()).isEqualTo(indoor);
    }

    @Test
    void rejectsPlaceWithoutName() {
        Place place = new Place(null, "문화", "부산광역시 테스트 주소",
                35.1796, 129.0756, true);

        assertThatThrownBy(() -> placeRepository.saveAndFlush(place))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
