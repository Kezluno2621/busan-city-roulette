package com.cityroulette.domain.course.repository;

import com.cityroulette.domain.member.entity.Member;
import com.cityroulette.domain.course.entity.SavedCourse;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SavedCourseRepository extends JpaRepository<SavedCourse, Long> {
    Page<SavedCourse> findByMemberAndTitleContainingIgnoreCase(Member member, String title, Pageable pageable);
    @EntityGraph(attributePaths = {"places", "places.place"})
    Optional<SavedCourse> findByIdAndMember(Long id, Member member);
}
