package com.cityroulette.service;

import com.cityroulette.domain.Place;
import com.cityroulette.repository.PlaceRepository;
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
}
