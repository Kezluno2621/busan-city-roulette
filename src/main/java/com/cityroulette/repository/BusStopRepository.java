package com.cityroulette.repository;
import com.cityroulette.domain.BusStop;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.util.*;
public interface BusStopRepository extends JpaRepository<BusStop,Long> {
    Optional<BusStop> findByExternalStopId(String id);
    List<BusStop> findAllByArsNumber(String arsNumber);
    Page<BusStop> findByNameContainingIgnoreCase(String name,Pageable pageable);
    List<BusStop> findByLatitudeIsNotNullAndLongitudeIsNotNull();

    @Query("""
            SELECT COUNT(s) FROM BusStop s
            WHERE s.latitude BETWEEN -90 AND 90
              AND s.longitude BETWEEN -180 AND 180
            """)
    long countWithValidCoordinates();

    @Query("""
            SELECT s FROM BusStop s
            WHERE s.latitude BETWEEN :minLatitude AND :maxLatitude
              AND s.longitude BETWEEN :minLongitude AND :maxLongitude
            """)
    List<BusStop> findWithinBoundingBox(
            @Param("minLatitude") BigDecimal minLatitude,
            @Param("maxLatitude") BigDecimal maxLatitude,
            @Param("minLongitude") BigDecimal minLongitude,
            @Param("maxLongitude") BigDecimal maxLongitude);
}
