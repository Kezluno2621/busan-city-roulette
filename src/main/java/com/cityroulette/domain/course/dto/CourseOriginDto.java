package com.cityroulette.domain.course.dto;

/**
 * 코스의 출발지 정보다.
 *
 * <p>{@code selectedByUser}가 {@code false}이면 위·경도는 내부 추천 기준점을 외부에
 * 출발지로 노출하지 않기 위해 {@code null}로 반환한다.</p>
 */
public record CourseOriginDto(
        Long placeId,
        String name,
        Double latitude,
        Double longitude,
        boolean selectedByUser) {

    /** 기존 테스트와 호출부에서 사용하는 명시적 출발지 생성자를 유지한다. */
    public CourseOriginDto(Long placeId, String name, double latitude, double longitude) {
        this(placeId, name, latitude, longitude, true);
    }

    public static CourseOriginDto unselected() {
        return new CourseOriginDto(null, "출발지 미지정", null, null, false);
    }
}
