package com.cityroulette.repository;

import com.cityroulette.domain.Place;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaceRepository extends JpaRepository<Place, Long> {

    boolean existsByExternalId(String externalId);
}
