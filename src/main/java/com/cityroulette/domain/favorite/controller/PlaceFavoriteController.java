package com.cityroulette.domain.favorite.controller;

import com.cityroulette.domain.favorite.dto.FavoriteCreateRequest;
import com.cityroulette.domain.favorite.dto.FavoritePageResponse;
import com.cityroulette.domain.favorite.dto.FavoriteResponse;
import com.cityroulette.domain.favorite.service.PlaceFavoriteService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import java.security.Principal;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/** 회원의 장소 즐겨찾기를 등록·조회·삭제합니다. */
@RestController
@RequestMapping("/api/favorites")
@Validated
public class PlaceFavoriteController {
    private final PlaceFavoriteService favorites;
    public PlaceFavoriteController(PlaceFavoriteService favorites) { this.favorites = favorites; }

    @PostMapping
    public ResponseEntity<FavoriteResponse> add(@Valid @RequestBody FavoriteCreateRequest request, Principal principal) {
        var result = favorites.add(principal.getName(), request.placeId());
        if (!result.created()) return ResponseEntity.ok(result.favorite());
        return ResponseEntity.created(URI.create("/api/favorites/" + request.placeId())).body(result.favorite());
    }

    @GetMapping
    public FavoritePageResponse list(@RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size, Principal principal) {
        return favorites.list(principal.getName(), page, size);
    }

    @DeleteMapping("/{placeId}")
    public ResponseEntity<Void> remove(@PathVariable Long placeId, Principal principal) {
        favorites.remove(principal.getName(), placeId);
        return ResponseEntity.noContent().build();
    }
}
