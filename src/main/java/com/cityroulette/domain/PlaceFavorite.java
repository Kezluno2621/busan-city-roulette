package com.cityroulette.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 회원별 장소 즐겨찾기의 저장 구조를 표현하는 JPA 엔티티입니다. 식별자와 컬럼 매핑을 정의합니다.
 */
@Entity
@Table(name = "place_favorites", uniqueConstraints =
        @UniqueConstraint(name = "uk_place_favorites_member_place", columnNames = {"member_id", "place_id"}))
public class PlaceFavorite {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "place_id", nullable = false)
    private Place place;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected PlaceFavorite() { }

    public PlaceFavorite(Member member, Place place) {
        this.member = member;
        this.place = place;
    }

    @PrePersist
    void create() { createdAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public Member getMember() { return member; }
    public Place getPlace() { return place; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
