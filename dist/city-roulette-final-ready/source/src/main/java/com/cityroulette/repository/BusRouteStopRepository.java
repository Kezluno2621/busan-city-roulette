package com.cityroulette.repository;
import com.cityroulette.domain.BusRouteStop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.*;
public interface BusRouteStopRepository extends JpaRepository<BusRouteStop,Long> {
    Optional<BusRouteStop> findByBusRouteIdAndStopSequence(Long routeId,int sequence);
    List<BusRouteStop> findByBusRouteIdOrderByStopSequence(Long routeId);
    List<BusRouteStop> findByBusStopIdOrderByBusRouteRouteNumberAsc(Long stopId);

    @Query("""
            SELECT rs FROM BusRouteStop rs
            JOIN FETCH rs.busRoute r
            WHERE rs.busStop.id IN :stopIds
            ORDER BY rs.busStop.id, r.routeNumber, r.id
            """)
    List<BusRouteStop> findWithRoutesByBusStopIds(@Param("stopIds") Collection<Long> stopIds);

    @Query("SELECT COUNT(r) FROM BusRoute r WHERE NOT EXISTS (SELECT rs.id FROM BusRouteStop rs WHERE rs.busRoute=r)")
    long countRoutesWithoutStops();

    @Query("SELECT COUNT(DISTINCT rs.busRoute.id) FROM BusRouteStop rs")
    long countRoutesWithStops();

    @Query("""
            SELECT DISTINCT s FROM BusRouteStop rs
            JOIN rs.busStop s
            WHERE rs.busRoute.id=:routeId
              AND (s.latitude IS NULL OR s.longitude IS NULL)
            ORDER BY s.id
            """)
    List<com.cityroulette.domain.BusStop> findStopsMissingCoordinates(@Param("routeId") Long routeId);

    @Query("""
            SELECT rs FROM BusRouteStop rs
            JOIN FETCH rs.busRoute
            JOIN FETCH rs.busStop
            WHERE rs.busRoute.id=:routeId
            ORDER BY rs.stopSequence
            """)
    List<BusRouteStop> findGraphByRouteId(@Param("routeId") Long routeId);

    @Query("""
            SELECT rs FROM BusRouteStop rs
            JOIN FETCH rs.busRoute
            JOIN FETCH rs.busStop
            WHERE rs.busRoute.id IN :routeIds
            ORDER BY rs.busRoute.id,rs.stopSequence
            """)
    List<BusRouteStop> findGraphsByRouteIds(@Param("routeIds") Collection<Long> routeIds);
}
