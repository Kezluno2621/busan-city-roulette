package com.cityroulette.dto.favorite;

import java.util.List;

public record FavoritePageResponse(List<FavoriteResponse> content, int page, int size,
        long totalElements, int totalPages, boolean first, boolean last) { }
