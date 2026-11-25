package com.cityroulette.service;

import com.cityroulette.domain.*;
import com.cityroulette.repository.*;
import java.util.List;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class PlaceFavoriteServiceTest {
    @Autowired PlaceFavoriteService service;
    @Autowired PlaceFavoriteRepository favorites;
    @Autowired MemberRepository members;
    @Autowired PlaceRepository places;
    Member firstMember;
    Member secondMember;
    Place place;

    @BeforeEach void setUp() {
        favorites.deleteAll();
        String suffix = Long.toString(System.nanoTime());
        firstMember = members.save(new Member("favA" + suffix, "encoded", "favA" + suffix + "@test.local", "회원A"));
        secondMember = members.save(new Member("favB" + suffix, "encoded", "favB" + suffix + "@test.local", "회원B"));
        place = places.save(new Place("즐겨찾기 장소 " + suffix, "관광지", "부산", 35.1, 129.1, false));
    }

    @Test void addsOnlyOnceAndPreservesOriginalPlace() {
        long placeCount = places.count();
        var first = service.add(firstMember.getUsername(), place.getId());
        var duplicate = service.add(firstMember.getUsername(), place.getId());

        assertThat(first.created()).isTrue();
        assertThat(duplicate.created()).isFalse();
        assertThat(duplicate.favorite().id()).isEqualTo(first.favorite().id());
        assertThat(favorites.count()).isEqualTo(1);
        assertThat(places.count()).isEqualTo(placeCount);
    }

    @Test void isolatesMembersAndDeletesOnlyOwnedFavorite() {
        service.add(firstMember.getUsername(), place.getId());
        service.remove(secondMember.getUsername(), place.getId());

        assertThat(service.list(firstMember.getUsername(), 0, 10).content()).hasSize(1);
        assertThat(service.list(secondMember.getUsername(), 0, 10).content()).isEmpty();
        service.remove(firstMember.getUsername(), place.getId());
        assertThat(favorites.count()).isZero();
        assertThat(places.findById(place.getId())).isPresent();
    }

    @Test void rejectsMissingPlaceAndReportsFavoriteIds() {
        assertThatThrownBy(() -> service.add(firstMember.getUsername(), Long.MAX_VALUE))
                .isInstanceOf(PlaceNotFoundException.class);
        service.add(firstMember.getUsername(), place.getId());
        assertThat(service.favoritePlaceIds(firstMember.getUsername(), List.of(place.getId(), Long.MAX_VALUE)))
                .containsExactly(place.getId());
        assertThat(service.favoritePlaceIds(null, List.of(place.getId()))).isEmpty();
    }

    @Test void concurrentAddsStillProduceOneDatabaseRow() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(6);
        CountDownLatch ready = new CountDownLatch(6);
        CountDownLatch start = new CountDownLatch(1);
        try {
            var tasks = java.util.stream.IntStream.range(0, 6).mapToObj(index ->
                    (Callable<Void>) () -> {
                        ready.countDown();
                        start.await(5, TimeUnit.SECONDS);
                        try { service.add(firstMember.getUsername(), place.getId()); }
                        catch (RuntimeException ignored) { /* UNIQUE 충돌도 중복 방지 성공으로 취급합니다. */ }
                        return null;
                    }).toList();
            var futures = tasks.stream().map(executor::submit).toList();
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            for (var future : futures) future.get(10, TimeUnit.SECONDS);
            assertThat(favorites.count()).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }
}
