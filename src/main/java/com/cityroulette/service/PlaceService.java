package com.cityroulette.service;

import com.cityroulette.domain.Place;
import com.cityroulette.dto.place.PlacePageResponse;
import com.cityroulette.dto.place.PlaceResponse;
import com.cityroulette.repository.PlaceRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PlaceService {

    private final PlaceRepository placeRepository;

    public PlaceService(PlaceRepository placeRepository) {
        this.placeRepository = placeRepository;
    }

    @Transactional(readOnly = true)
    public List<Place> getPlaces(int page, int size) {
        PageRequest pageRequest = PageRequest.of(page - 1, size, Sort.by("id").ascending());
        return placeRepository.findAll(pageRequest).getContent();
    }

    @Transactional(readOnly = true)
    public PlacePageResponse searchPlaces(
            String category,
            String district,
            String keyword,
            int page,
            int size) {
        PageRequest pageRequest = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Order.asc("name"), Sort.Order.asc("id")));
        Page<PlaceResponse> result = placeRepository.search(
                        normalize(category),
                        normalize(district),
                        normalize(keyword),
                        pageRequest)
                .map(PlaceResponse::from);
        return PlacePageResponse.from(result);
    }

    @Transactional(readOnly = true)
    public PlaceResponse getPlace(Long id) {
        return placeRepository.findById(id)
                .map(PlaceResponse::from)
                .orElseThrow(() -> new PlaceNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<String> getCategories() {
        return placeRepository.findDistinctCategories();
    }

    @Transactional(readOnly = true)
    public List<String> getDistricts() {
        return placeRepository.findDistinctDistricts();
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
