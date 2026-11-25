package com.cityroulette.dto.place;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 목록과 페이지 정보를 함께 전달해 호출자가 다음 페이지를 조회할 수 있게 합니다.
 */
public record PlacePageResponse(
        List<PlaceResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last) {

    public static PlacePageResponse from(Page<PlaceResponse> result) {
        return new PlacePageResponse(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.isFirst(),
                result.isLast());
    }
}
