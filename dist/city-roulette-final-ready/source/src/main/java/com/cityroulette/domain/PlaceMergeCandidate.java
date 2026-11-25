package com.cityroulette.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "place_merge_candidates", uniqueConstraints = @UniqueConstraint(
        name = "uk_merge_candidate_pair", columnNames = {"place_id", "candidate_place_id"}))
public class PlaceMergeCandidate {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "place_id") private Place place;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "candidate_place_id") private Place candidatePlace;
    @Column(nullable = false, length = 500) private String reason;
    @Column(nullable = false, length = 20) private String status = "PENDING";
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt = LocalDateTime.now();
    protected PlaceMergeCandidate() { }
    public PlaceMergeCandidate(Place place, Place candidatePlace, String reason) {
        this.place = place; this.candidatePlace = candidatePlace; this.reason = reason;
    }
    public Long getId() { return id; }
    public Place getPlace() { return place; }
    public Place getCandidatePlace() { return candidatePlace; }
    public String getReason() { return reason; }
    public String getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
