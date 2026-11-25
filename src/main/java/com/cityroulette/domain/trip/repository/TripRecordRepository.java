package com.cityroulette.domain.trip.repository;

import com.cityroulette.domain.member.entity.Member;
import com.cityroulette.domain.trip.entity.TripRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 여행 기록 엔티티를 관리합니다. 회원과 기록 ID를 함께 조회하는 메서드가 소유자 범위 검색에 사용됩니다.
 */
public interface TripRecordRepository extends JpaRepository<TripRecord, Long> {

    List<TripRecord> findByMemberOrderByCreatedAtDesc(Member member);

    Optional<TripRecord> findByIdAndMember(Long id, Member member);
}
