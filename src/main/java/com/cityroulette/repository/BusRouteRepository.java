package com.cityroulette.repository;
import com.cityroulette.domain.BusRoute;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface BusRouteRepository extends JpaRepository<BusRoute,Long> {
    Optional<BusRoute> findByExternalRouteId(String id);
    List<BusRoute> findAllByRouteNumberIgnoreCase(String routeNumber);
    Page<BusRoute> findByRouteNumberContainingIgnoreCase(String number,Pageable pageable);
}
