package com.cityroulette.service;

import com.cityroulette.domain.*;
import com.cityroulette.dto.course.*;
import com.cityroulette.dto.savedcourse.*;
import com.cityroulette.repository.*;
import com.cityroulette.service.GeneratedCourseRegistry.GeneratedCourseSnapshot;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 로그인 회원이 서버에서 생성된 코스 스냅샷을 저장하고 관리하도록 돕는 서비스입니다.
 *
 * <p>일회용 생성 토큰, 회원 소유권, 낙관적 잠금 버전을 검증한 뒤 저장하며 원본 장소가
 * 변경되어도 당시 결과를 보여줄 수 있도록 주요 값을 스냅샷으로 보존합니다.</p>
 */
@Service
public class SavedCourseService {
    private final SavedCourseRepository courses;
    private final MemberRepository members;
    private final PlaceRepository places;
    private final GeneratedCourseRegistry generatedCourses;
    private final ObjectMapper objectMapper;
    private final PlaceImagePolicyService images;

    public SavedCourseService(SavedCourseRepository courses, MemberRepository members,
            PlaceRepository places, GeneratedCourseRegistry generatedCourses, ObjectMapper objectMapper,
            PlaceImagePolicyService images) {
        this.courses = courses; this.members = members; this.places = places;
        this.generatedCourses = generatedCourses; this.objectMapper = objectMapper;
        this.images = images;
    }

    @Transactional
    public SavedCourseResponse save(String username, SavedCourseCreateRequest request) {
        Member member = requireMember(username);
        GeneratedCourseSnapshot snapshot = generatedCourses.consume(request.generationToken());
        validate(snapshot);
        CourseGenerationRequest generation = snapshot.request();
        CourseGenerationResponse response = snapshot.response();
        SavedCourse course = new SavedCourse(member, request.title().trim(), response.origin().name(),
                decimal(response.origin().latitude()), decimal(response.origin().longitude()),
                response.requestedPlaceCount(), response.actualPlaceCount(),
                generation.randomness() == null ? 0 : generation.randomness(), generation.availableMinutes(),
                generation.budget(), json(response.dataLimitations()));
        Map<Long, Place> sourcePlaces = places.findAllById(response.places().stream()
                        .map(CoursePlaceDto::placeId).toList()).stream()
                .collect(Collectors.toMap(Place::getId, Function.identity()));
        for (CoursePlaceDto item : response.places()) {
            course.addPlace(new SavedCoursePlace(course, sourcePlaces.get(item.placeId()), item.sequence(),
                    item.placeName(), item.category(), item.address(), decimal(item.latitude()), decimal(item.longitude()),
                    decimal(item.recommendationScore()), json(item.recommendationReasons()),
                    decimal(item.distanceFromPreviousMeters()), item.transitVerificationStatus(),
                    json(item.dataLimitations())));
        }
        return detail(courses.saveAndFlush(course));
    }

    @Transactional(readOnly = true)
    public SavedCoursePageResponse list(String username, int page, int size, String title) {
        Member member = requireMember(username);
        Page<SavedCourse> result = courses.findByMemberAndTitleContainingIgnoreCase(member,
                title == null ? "" : title.trim(), PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return new SavedCoursePageResponse(result.getContent().stream().map(this::summary).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public SavedCourseResponse findOne(String username, Long id) { return detail(owned(username, id)); }

    @Transactional
    public SavedCourseResponse rename(String username, Long id, SavedCourseTitleUpdateRequest request) {
        SavedCourse course = owned(username, id);
        if (!Objects.equals(course.getVersion(), request.version())) throw new SavedCourseConflictException();
        course.rename(request.title().trim());
        try {
            courses.flush();
        } catch (OptimisticLockingFailureException exception) {
            throw new SavedCourseConflictException();
        }
        return detail(course);
    }

    @Transactional
    public void delete(String username, Long id) { courses.delete(owned(username, id)); }

    private SavedCourse owned(String username, Long id) {
        return courses.findByIdAndMember(id, requireMember(username)).orElseThrow(SavedCourseNotFoundException::new);
    }

    private Member requireMember(String username) {
        if (username == null) throw new SavedCourseNotFoundException();
        return members.findByUsername(username).orElseThrow(SavedCourseNotFoundException::new);
    }

    private void validate(GeneratedCourseSnapshot snapshot) {
        CourseGenerationResponse response = snapshot.response();
        if (response == null || response.places() == null || response.places().isEmpty()
                || response.places().size() > 5 || response.actualPlaceCount() != response.places().size()) {
            throw new IllegalArgumentException("저장할 코스의 장소 수가 올바르지 않습니다.");
        }
        Set<Long> ids = new HashSet<>(); Set<Integer> orders = new HashSet<>();
        for (CoursePlaceDto item : response.places()) {
            if (item.placeId() == null || !ids.add(item.placeId()) || !orders.add(item.sequence())
                    || item.sequence() < 1 || item.sequence() > response.places().size()
                    || !coordinate(item.latitude(), item.longitude())
                    || item.recommendationScore() < 0 || item.recommendationScore() > 100
                    || !Double.isFinite(item.distanceFromPreviousMeters()) || item.distanceFromPreviousMeters() < 0) {
                throw new IllegalArgumentException("서버가 생성한 코스 스냅샷이 유효하지 않습니다.");
            }
        }
        for (int order = 1; order <= response.places().size(); order++) {
            if (!orders.contains(order)) throw new IllegalArgumentException("방문 순서가 연속적이지 않습니다.");
        }
    }

    private boolean coordinate(Double latitude, Double longitude) {
        return latitude != null && longitude != null && Double.isFinite(latitude) && Double.isFinite(longitude)
                && latitude >= -90 && latitude <= 90 && longitude >= -180 && longitude <= 180;
    }

    private SavedCourseSummaryResponse summary(SavedCourse course) {
        String representative = course.getPlaces().isEmpty() ? null : course.getPlaces().get(0).getPlaceNameSnapshot();
        return new SavedCourseSummaryResponse(course.getId(), course.getTitle(), course.getActualPlaceCount(),
                representative, course.getCreatedAt(), course.getUpdatedAt(), course.getVersion());
    }

    private SavedCourseResponse detail(SavedCourse course) {
        Set<Long> displayableImages = images.displayablePlaceIds(course.getPlaces().stream()
                .map(SavedCoursePlace::getPlace).filter(Objects::nonNull).map(Place::getId).toList());
        return new SavedCourseResponse(course.getId(), course.getTitle(), course.getOriginName(),
                course.getOriginLatitude().doubleValue(), course.getOriginLongitude().doubleValue(),
                course.getRequestedPlaceCount(), course.getActualPlaceCount(), course.getRandomness(),
                course.getAvailableMinutes(), course.getBudget(), course.getEstimateTimeStatus(),
                course.getEstimateCostStatus(), list(course.getDataLimitations()),
                course.getPlaces().stream().map(item -> place(item, displayableImages)).toList(), course.getCreatedAt(),
                course.getUpdatedAt(), course.getVersion());
    }

    private SavedCoursePlaceResponse place(SavedCoursePlace item, Set<Long> displayableImages) {
        Long placeId = item.getPlace() == null ? null : item.getPlace().getId();
        String imageUrl = placeId != null && displayableImages.contains(placeId)
                ? item.getPlace().getImageUrl() : null;
        return new SavedCoursePlaceResponse(placeId,
                item.getVisitOrder(), item.getPlaceNameSnapshot(), item.getCategorySnapshot(),
                item.getAddressSnapshot(), item.getLatitudeSnapshot().doubleValue(),
                item.getLongitudeSnapshot().doubleValue(), item.getRecommendationScore().doubleValue(),
                list(item.getRecommendationReason()), item.getDistanceFromPreviousMeters().doubleValue(),
                item.getTransitVerificationStatus(), list(item.getDataLimitations()), imageUrl);
    }

    private BigDecimal decimal(double value) { return BigDecimal.valueOf(value); }
    private String json(List<String> values) {
        try { return objectMapper.writeValueAsString(values == null ? List.of() : values); }
        catch (JsonProcessingException exception) { throw new IllegalStateException("스냅샷 직렬화 실패", exception); }
    }
    private List<String> list(String value) {
        if (value == null || value.isBlank()) return List.of();
        try { return objectMapper.readValue(value, new TypeReference<>() { }); }
        catch (JsonProcessingException exception) { return List.of("저장된 제한사항을 읽지 못했습니다."); }
    }
}
