package com.cityroulette.service;

import com.cityroulette.domain.*;
import com.cityroulette.dto.course.*;
import com.cityroulette.dto.recommendation.RecommendationTransitStatus;
import com.cityroulette.dto.savedcourse.*;
import com.cityroulette.repository.*;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class SavedCourseServiceTest {
    @Autowired SavedCourseService service;
    @Autowired GeneratedCourseRegistry registry;
    @Autowired SavedCourseRepository courses;
    @Autowired SavedCoursePlaceRepository coursePlaces;
    @Autowired MemberRepository members;
    @Autowired PlaceRepository places;
    Member owner; Member other; Place first; Place second;

    @BeforeEach void setUp() {
        coursePlaces.deleteAll(); courses.deleteAll();
        String suffix = Long.toString(System.nanoTime());
        owner = members.save(new Member("owner" + suffix, "encoded", "owner" + suffix + "@test.local", "소유자"));
        other = members.save(new Member("other" + suffix, "encoded", "other" + suffix + "@test.local", "다른회원"));
        first = places.save(new Place("첫 장소 " + suffix, "관광지", "부산", 35.10, 129.10, false));
        second = places.save(new Place("둘째 장소 " + suffix, "카페", "부산", 35.11, 129.11, false));
    }

    @Test void savesTrustedSnapshotWithOrderAndPreservesSourcePlaces() {
        long placeCount = places.count();
        var saved = save(owner, response(List.of(place(first, 1), place(second, 2))));

        assertThat(saved.places()).extracting(x -> x.visitOrder()).containsExactly(1, 2);
        assertThat(saved.places()).extracting(x -> x.placeName()).containsExactly(first.getName(), second.getName());
        assertThat(saved.version()).isZero();
        assertThat(courses.count()).isEqualTo(1);
        assertThat(coursePlaces.count()).isEqualTo(2);
        assertThat(places.count()).isEqualTo(placeCount);
        assertThat(service.list(owner.getUsername(), 0, 10, "나의").content()).hasSize(1);
    }

    @Test void hidesCourseFromOtherMemberForReadUpdateAndDelete() {
        var saved = save(owner, response(List.of(place(first, 1))));

        assertThat(service.list(other.getUsername(), 0, 10, "").content()).isEmpty();
        assertThatThrownBy(() -> service.findOne(other.getUsername(), saved.id())).isInstanceOf(SavedCourseNotFoundException.class);
        assertThatThrownBy(() -> service.rename(other.getUsername(), saved.id(),
                new SavedCourseTitleUpdateRequest("탈취", saved.version()))).isInstanceOf(SavedCourseNotFoundException.class);
        assertThatThrownBy(() -> service.delete(other.getUsername(), saved.id())).isInstanceOf(SavedCourseNotFoundException.class);
        assertThat(courses.count()).isEqualTo(1);
    }

    @Test void renamesWithOptimisticVersionAndRejectsStaleUpdate() {
        var saved = save(owner, response(List.of(place(first, 1))));
        var renamed = service.rename(owner.getUsername(), saved.id(), new SavedCourseTitleUpdateRequest("새 제목", saved.version()));

        assertThat(renamed.title()).isEqualTo("새 제목");
        assertThat(renamed.version()).isGreaterThan(saved.version());
        assertThatThrownBy(() -> service.rename(owner.getUsername(), saved.id(),
                new SavedCourseTitleUpdateRequest("오래된 수정", saved.version())))
                .isInstanceOf(SavedCourseConflictException.class);
    }

    @Test void deletesChildrenButNeverDeletesOriginalPlaces() {
        var saved = save(owner, response(List.of(place(first, 1), place(second, 2))));
        long placeCount = places.count();

        service.delete(owner.getUsername(), saved.id());

        assertThat(courses.count()).isZero();
        assertThat(coursePlaces.count()).isZero();
        assertThat(places.count()).isEqualTo(placeCount);
    }

    @Test void rejectsFakeTokenAndInvalidSnapshotWithoutPartialDatabaseWrite() {
        assertThatThrownBy(() -> service.save(owner.getUsername(),
                new SavedCourseCreateRequest("조작", "client-made-token")))
                .isInstanceOf(InvalidGeneratedCourseException.class);
        CoursePlaceDto duplicateA = place(first, 1);
        CoursePlaceDto duplicateB = new CoursePlaceDto(first.getId(), "조작", "카페", "주소", 35.2, 129.2,
                2, 100, List.of("조작"), 10, RecommendationTransitStatus.VERIFIED,
                CourseEstimateStatus.UNKNOWN, CourseEstimateStatus.UNKNOWN, List.of());
        CourseGenerationResponse invalid = response(List.of(duplicateA, duplicateB));
        String token = registry.issue(request(), invalid);

        assertThatThrownBy(() -> service.save(owner.getUsername(), new SavedCourseCreateRequest("조작", token)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(courses.count()).isZero();
        assertThat(coursePlaces.count()).isZero();
    }

    @Test void generationTokenIsOneTimeUse() {
        CourseGenerationResponse response = response(List.of(place(first, 1)));
        String token = registry.issue(request(), response);
        service.save(owner.getUsername(), new SavedCourseCreateRequest("첫 저장", token));
        assertThatThrownBy(() -> service.save(owner.getUsername(), new SavedCourseCreateRequest("재사용", token)))
                .isInstanceOf(InvalidGeneratedCourseException.class);
        assertThat(courses.count()).isEqualTo(1);
    }

    private SavedCourseResponse save(Member member, CourseGenerationResponse response) {
        String token = registry.issue(request(), response);
        return service.save(member.getUsername(), new SavedCourseCreateRequest("나의 코스", token));
    }
    private CourseGenerationRequest request() {
        return new CourseGenerationRequest(null, 35.09, 129.09, List.of(), null, 180, 30000L, 60, 2, 42L);
    }
    private CourseGenerationResponse response(List<CoursePlaceDto> items) {
        return new CourseGenerationResponse("fixture", new CourseOriginDto(null, "출발지", 35.09, 129.09),
                Instant.parse("2026-10-29T00:00:00Z"), items.size(), items.size(), items, false,
                "fixture", List.of("직선거리"), null);
    }
    private CoursePlaceDto place(Place place, int order) {
        return new CoursePlaceDto(place.getId(), place.getName(), place.getCategory(), place.getAddress(),
                place.getLatitude(), place.getLongitude(), order, 88.5, List.of("추천 이유"), 120.0 * order,
                RecommendationTransitStatus.PARTIAL_DATA, CourseEstimateStatus.UNKNOWN,
                CourseEstimateStatus.UNKNOWN, List.of("부분 데이터"));
    }
}
