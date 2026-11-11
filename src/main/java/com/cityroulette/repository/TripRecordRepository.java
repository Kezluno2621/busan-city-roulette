package com.cityroulette.repository;

import com.cityroulette.domain.Member;
import com.cityroulette.domain.TripRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TripRecordRepository extends JpaRepository<TripRecord, Long> {

    List<TripRecord> findByMemberOrderByCreatedAtDesc(Member member);

    Optional<TripRecord> findByIdAndMember(Long id, Member member);
}
