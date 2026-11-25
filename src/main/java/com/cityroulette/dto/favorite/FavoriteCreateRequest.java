package com.cityroulette.dto.favorite;

import jakarta.validation.constraints.NotNull;

/**
 * 호출자가 보내는 입력의 계약입니다. 값은 선언된 필드와 검증 규칙에 따라 해석합니다.
 */
public record FavoriteCreateRequest(@NotNull Long placeId) { }
