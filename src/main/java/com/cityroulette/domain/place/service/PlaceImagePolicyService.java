package com.cityroulette.domain.place.service;

import com.cityroulette.domain.place.entity.Place;
import com.cityroulette.domain.place.repository.PlaceRepository;
import com.cityroulette.domain.place.repository.PlaceSourceRepository;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PlaceImagePolicyService {
    private final PlaceSourceRepository sources;
    private final PlaceRepository places;
    public PlaceImagePolicyService(PlaceSourceRepository sources, PlaceRepository places) {
        this.sources = sources;
        this.places = places;
    }

    public Set<Long> displayablePlaceIds(Collection<Long> placeIds) {
        if (placeIds == null || placeIds.isEmpty()) return Set.of();
        Set<Long> licensed = Set.copyOf(sources.findPlaceIdsWithKnownImageLicense(placeIds));
        Set<Long> displayable = new HashSet<>();
        for (Place place : places.findAllById(licensed)) {
            if (isSafePublicImageUrl(place.getImageUrl())) displayable.add(place.getId());
        }
        return Set.copyOf(displayable);
    }

    public boolean canDisplay(Long placeId) { return displayablePlaceIds(java.util.List.of(placeId)).contains(placeId); }

    boolean isSafePublicImageUrl(String value) {
        if (value == null || value.isBlank()) return false;
        try {
            URI uri = new URI(value.trim());
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
            String normalizedHost = host.replace("[", "").replace("]", "");
            if (!(scheme.equals("https") || scheme.equals("http")) || host.isBlank()) return false;
            if (normalizedHost.equals("localhost") || normalizedHost.endsWith(".localhost")
                    || normalizedHost.endsWith(".local") || normalizedHost.endsWith(".internal")
                    || normalizedHost.equals("::1") || normalizedHost.startsWith("fe80:")
                    || normalizedHost.startsWith("fc") || normalizedHost.startsWith("fd")
                    || normalizedHost.startsWith("127.") || normalizedHost.startsWith("10.")
                    || normalizedHost.startsWith("169.254.") || normalizedHost.startsWith("192.168.")) return false;
            if (normalizedHost.matches("172\\.(1[6-9]|2[0-9]|3[01])\\..*")) return false;
            return !normalizedHost.startsWith("0.");
        } catch (URISyntaxException exception) {
            return false;
        }
    }
}
