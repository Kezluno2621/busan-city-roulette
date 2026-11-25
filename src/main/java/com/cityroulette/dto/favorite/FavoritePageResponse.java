package com.cityroulette.dto.favorite;

import java.util.List;

/**
 * 목록과 페이지 정보를 함께 전달해 호출자가 다음 페이지를 조회할 수 있게 합니다.
 */
public record FavoritePageResponse(List<FavoriteResponse> content, int page, int size,
        long totalElements, int totalPages, boolean first, boolean last) { }
