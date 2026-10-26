package com.cityroulette.repository;

import com.cityroulette.domain.Place;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlaceRepository extends JpaRepository<Place, Long> {

    boolean existsByExternalId(String externalId);

    Optional<Place> findByExternalId(String externalId);

    List<Place> findByLatitudeIsNotNullAndLongitudeIsNotNull();
}
