package com.cityroulette.dto.favorite;

import jakarta.validation.constraints.NotNull;

public record FavoriteCreateRequest(@NotNull Long placeId) { }
