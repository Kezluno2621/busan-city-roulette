package com.cityroulette.repository;

import com.cityroulette.domain.PlaceMergeCandidate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaceMergeCandidateRepository extends JpaRepository<PlaceMergeCandidate, Long> {
    boolean existsByPlaceIdAndCandidatePlaceId(Long placeId, Long candidatePlaceId);
}
