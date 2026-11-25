package com.cityroulette.domain.place.repository;

import com.cityroulette.domain.place.entity.PlaceMergeCandidate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaceMergeCandidateRepository extends JpaRepository<PlaceMergeCandidate, Long> {
    boolean existsByPlaceIdAndCandidatePlaceId(Long placeId, Long candidatePlaceId);
}
