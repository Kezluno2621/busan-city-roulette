package com.cityroulette.domain.course.repository;

import com.cityroulette.domain.course.entity.SavedCoursePlace;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SavedCoursePlaceRepository extends JpaRepository<SavedCoursePlace, Long> { }
