package com.cityroulette.repository;

import com.cityroulette.domain.PlaceFavorite;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 회원과 장소를 키로 즐겨찾기 목록 및 등록 여부를 조회합니다. 회원별로 조회 범위를 구분합니다.
 */
public interface PlaceFavoriteRepository extends JpaRepository<PlaceFavorite, Long> {
    @EntityGraph(attributePaths = "place")
    Page<PlaceFavorite> findByMemberUsername(String username, Pageable pageable);

    @EntityGraph(attributePaths = "place")
    Optional<PlaceFavorite> findByMemberUsernameAndPlaceId(String username, Long placeId);

    boolean existsByMemberUsernameAndPlaceId(String username, Long placeId);

    @Query("select f.place.id from PlaceFavorite f where f.member.username = :username and f.place.id in :placeIds")
    List<Long> findPlaceIds(@Param("username") String username, @Param("placeIds") Collection<Long> placeIds);

    @Modifying
    @Query("delete from PlaceFavorite f where f.member.username = :username and f.place.id = :placeId")
    int deleteOwned(@Param("username") String username, @Param("placeId") Long placeId);
}
