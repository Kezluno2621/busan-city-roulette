package com.cityroulette.controller;

import com.cityroulette.service.PlaceFavoriteService;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/my/favorites")
public class PlaceFavoritePageController {
    private final PlaceFavoriteService favorites;
    public PlaceFavoritePageController(PlaceFavoriteService favorites) { this.favorites = favorites; }

    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page, Principal principal, Model model) {
        model.addAttribute("favorites", favorites.list(principal.getName(), Math.max(0, page), 12));
        return "my/favorites";
    }
}
