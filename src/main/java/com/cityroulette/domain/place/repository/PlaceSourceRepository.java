package com.cityroulette.domain.place.repository;

import com.cityroulette.domain.place.entity.PlaceSource;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;
import java.util.Collection;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlaceSourceRepository extends JpaRepository<PlaceSource, Long> {
    Optional<PlaceSource> findByProviderAndExternalId(String provider, String externalId);
    boolean existsByProviderAndExternalId(String provider, String externalId);
    List<PlaceSource> findByPlaceIdOrderByProviderAsc(Long placeId);
    @Query("select distinct s.place.id from PlaceSource s where s.place.id in :placeIds and s.imageLicense is not null and trim(s.imageLicense) <> ''")
    List<Long> findPlaceIdsWithKnownImageLicense(@Param("placeIds") Collection<Long> placeIds);
}
