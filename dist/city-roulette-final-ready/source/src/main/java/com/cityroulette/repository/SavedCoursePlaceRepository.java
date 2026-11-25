package com.cityroulette.repository;

import com.cityroulette.domain.SavedCoursePlace;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SavedCoursePlaceRepository extends JpaRepository<SavedCoursePlace, Long> { }
