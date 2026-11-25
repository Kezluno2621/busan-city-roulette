package com.cityroulette.domain.place.repository;

import com.cityroulette.domain.place.entity.Place;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PlaceRepository extends JpaRepository<Place, Long> {

    boolean existsByExternalId(String externalId);

    Optional<Place> findByExternalId(String externalId);

    boolean existsBySourceAndExternalId(String source, String externalId);

    Optional<Place> findBySourceAndExternalId(String source, String externalId);

    @Query("""
            SELECT p FROM Place p
            WHERE (:category IS NULL OR LOWER(p.category) = LOWER(:category))
              AND (:district IS NULL OR p.district = :district)
              AND (:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """)
    Page<Place> search(
            @Param("category") String category,
            @Param("district") String district,
            @Param("keyword") String keyword,
            Pageable pageable);

    @Query("SELECT DISTINCT p.category FROM Place p WHERE p.category IS NOT NULL ORDER BY p.category")
    List<String> findDistinctCategories();

    @Query("SELECT DISTINCT p.district FROM Place p WHERE p.district IS NOT NULL ORDER BY p.district")
    List<String> findDistinctDistricts();

    List<Place> findByLatitudeIsNotNullAndLongitudeIsNotNull();

    @Query("SELECT p.category, COUNT(p) FROM Place p GROUP BY p.category ORDER BY p.category")
    List<Object[]> countByCategory();

    @Query("SELECT p.district, COUNT(p) FROM Place p GROUP BY p.district ORDER BY p.district")
    List<Object[]> countByDistrict();

    @Query("SELECT p.id FROM Place p ORDER BY p.id")
    List<Long> findAllIds();

    List<Place> findByNormalizedNameAndIdNot(String normalizedName, Long id);

    @Query("""
            SELECT p FROM Place p
            WHERE p.latitude IS NOT NULL
              AND p.longitude IS NOT NULL
              AND (p.mergeStatus IS NULL OR p.mergeStatus = 'ACTIVE')
              AND (:filterCategories = false OR LOWER(p.category) IN :categories)
              AND (:district IS NULL OR p.district = :district)
            ORDER BY p.id
            """)
    List<Place> findRecommendationCandidates(
            @Param("filterCategories") boolean filterCategories,
            @Param("categories") List<String> categories,
            @Param("district") String district,
            Pageable pageable);
}
