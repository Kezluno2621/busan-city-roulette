package com.cityroulette.service;

import com.cityroulette.domain.PlaceFavorite;
import com.cityroulette.dto.favorite.*;
import com.cityroulette.repository.*;
import java.util.Collection;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlaceFavoriteService {
    private final PlaceFavoriteRepository favorites;
    private final MemberRepository members;
    private final PlaceRepository places;
    private final PlaceImagePolicyService images;

    public PlaceFavoriteService(PlaceFavoriteRepository favorites, MemberRepository members, PlaceRepository places,
            PlaceImagePolicyService images) {
        this.favorites = favorites;
        this.members = members;
        this.places = places;
        this.images = images;
    }

    @Transactional
    public FavoriteAddResult add(String username, Long placeId) {
        var existing = favorites.findByMemberUsernameAndPlaceId(username, placeId);
        if (existing.isPresent()) return new FavoriteAddResult(
                FavoriteResponse.from(existing.get(), images.canDisplay(placeId)), false);
        var member = members.findByUsername(username).orElseThrow(FavoriteAccessException::new);
        var place = places.findById(placeId).orElseThrow(() -> new PlaceNotFoundException(placeId));
        var saved = favorites.saveAndFlush(new PlaceFavorite(member, place));
        return new FavoriteAddResult(FavoriteResponse.from(saved, images.canDisplay(placeId)), true);
    }

    @Transactional(readOnly = true)
    public FavoritePageResponse list(String username, int page, int size) {
        var result = favorites.findByMemberUsername(username,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        var allowedImages = images.displayablePlaceIds(result.getContent().stream().map(f -> f.getPlace().getId()).toList());
        return new FavoritePageResponse(result.getContent().stream()
                .map(f -> FavoriteResponse.from(f, allowedImages.contains(f.getPlace().getId()))).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages(),
                result.isFirst(), result.isLast());
    }

    @Transactional
    public void remove(String username, Long placeId) { favorites.deleteOwned(username, placeId); }

    @Transactional(readOnly = true)
    public boolean isFavorite(String username, Long placeId) {
        return username != null && favorites.existsByMemberUsernameAndPlaceId(username, placeId);
    }

    @Transactional(readOnly = true)
    public Set<Long> favoritePlaceIds(String username, Collection<Long> placeIds) {
        if (username == null || placeIds == null || placeIds.isEmpty()) return Set.of();
        return Set.copyOf(favorites.findPlaceIds(username, placeIds));
    }
}
