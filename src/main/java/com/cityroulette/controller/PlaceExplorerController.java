package com.cityroulette.controller;
import com.cityroulette.service.PlaceExplorerService;
import com.cityroulette.service.PlaceFavoriteService;
import com.cityroulette.service.PlaceImagePolicyService;
import jakarta.validation.constraints.*;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
/**
 * 장소 목록과 상세의 GET 요청을 받습니다. 서비스가 구성한 조회 결과를 Model에 넣어 Thymeleaf 화면에 전달합니다.
 */
@Controller @Validated
public class PlaceExplorerController {
    private final PlaceExplorerService service;
    private final PlaceFavoriteService favorites;
    private final PlaceImagePolicyService images;

    public PlaceExplorerController(PlaceExplorerService service, PlaceFavoriteService favorites,
            PlaceImagePolicyService images) {
        this.service = service;
        this.favorites = favorites;
        this.images = images;
    }

    @GetMapping("/places")
    public String list(@RequestParam(required=false) @Size(max=100) String keyword,
            @RequestParam(required=false) @Size(max=100) String category,
            @RequestParam(required=false) @Size(max=50) String district,
            @RequestParam(defaultValue="0") @Min(0) int page,
            @RequestParam(defaultValue="12") @Min(1) @Max(48) int size,
            Principal principal, Model model) {
        var result = service.search(category, district, keyword, page, size);
        model.addAttribute("result", result);
        model.addAttribute("categories", service.categories());
        model.addAttribute("districts", service.districts());
        model.addAttribute("keyword", keyword);
        model.addAttribute("category", category);
        model.addAttribute("district", district);
        model.addAttribute("pages", pageWindow(result.page(), result.totalPages()));
        model.addAttribute("favoritePlaceIds", favorites.favoritePlaceIds(
                principal == null ? null : principal.getName(), result.content().stream().map(p -> p.id()).toList()));
        model.addAttribute("displayableImagePlaceIds", images.displayablePlaceIds(
                result.content().stream().map(p -> p.id()).toList()));
        return "places/list";
    }

    @GetMapping("/places/{id}")
    public String detail(@PathVariable Long id, @RequestParam(defaultValue="500") @Min(50) @Max(3000) int radius,
            Principal principal, Model model) {
        model.addAttribute("detail", service.detail(id, radius));
        model.addAttribute("radius", radius);
        model.addAttribute("radii", java.util.List.of(300,500,1000,2000));
        model.addAttribute("favorite", favorites.isFavorite(principal == null ? null : principal.getName(), id));
        model.addAttribute("imageDisplayAllowed", images.canDisplay(id));
        return "places/detail";
    }

    static int[] pageWindow(int currentPage, int totalPages) {
        if (totalPages <= 0) return new int[0];
        int length = Math.min(5, totalPages);
        int start = Math.max(0, Math.min(currentPage - length / 2, totalPages - length));
        int[] pages = new int[length];
        for (int index = 0; index < length; index++) pages[index] = start + index;
        return pages;
    }
}
