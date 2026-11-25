package com.cityroulette.domain.favorite.dto;

import com.cityroulette.domain.favorite.entity.PlaceFavorite;
import java.time.LocalDateTime;

public record FavoriteResponse(Long id, Long placeId, String placeName, String category,
        String address, String district, Double latitude, Double longitude, String imageUrl,
        LocalDateTime createdAt) {
    public static FavoriteResponse from(PlaceFavorite favorite) {
        return from(favorite, true);
    }
    public static FavoriteResponse from(PlaceFavorite favorite, boolean imageAllowed) {
        var place = favorite.getPlace();
        return new FavoriteResponse(favorite.getId(), place.getId(), place.getName(), place.getCategory(),
                place.getAddress(), place.getDistrict(), place.getLatitude(), place.getLongitude(),
                imageAllowed ? place.getImageUrl() : null, favorite.getCreatedAt());
    }
}
