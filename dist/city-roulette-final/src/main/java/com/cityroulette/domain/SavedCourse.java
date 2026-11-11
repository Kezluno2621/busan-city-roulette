package com.cityroulette.domain;

import com.cityroulette.dto.course.CourseEstimateStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "saved_courses")
public class SavedCourse {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "member_id", nullable = false) private Member member;
    @Column(nullable = false, length = 100) private String title;
    @Column(name = "origin_name", nullable = false, length = 255) private String originName;
    @Column(name = "origin_latitude", nullable = false, precision = 10, scale = 7) private BigDecimal originLatitude;
    @Column(name = "origin_longitude", nullable = false, precision = 10, scale = 7) private BigDecimal originLongitude;
    @Column(name = "requested_place_count", nullable = false) private int requestedPlaceCount;
    @Column(name = "actual_place_count", nullable = false) private int actualPlaceCount;
    @Column(nullable = false) private int randomness;
    @Column(name = "available_minutes") private Integer availableMinutes;
    private Long budget;
    @Enumerated(EnumType.STRING) @Column(name = "estimate_time_status", nullable = false, length = 20) private CourseEstimateStatus estimateTimeStatus;
    @Enumerated(EnumType.STRING) @Column(name = "estimate_cost_status", nullable = false, length = 20) private CourseEstimateStatus estimateCostStatus;
    @Column(name = "data_limitations", length = 10000) private String dataLimitations;
    @OneToMany(mappedBy = "savedCourse", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("visitOrder ASC") private List<SavedCoursePlace> places = new ArrayList<>();
    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;
    @Version @Column(nullable = false) private Long version;

    protected SavedCourse() { }
    public SavedCourse(Member member, String title, String originName, BigDecimal originLatitude,
            BigDecimal originLongitude, int requestedPlaceCount, int actualPlaceCount, int randomness,
            Integer availableMinutes, Long budget, String dataLimitations) {
        this.member = member; this.title = title; this.originName = originName;
        this.originLatitude = originLatitude; this.originLongitude = originLongitude;
        this.requestedPlaceCount = requestedPlaceCount; this.actualPlaceCount = actualPlaceCount;
        this.randomness = randomness; this.availableMinutes = availableMinutes; this.budget = budget;
        this.estimateTimeStatus = CourseEstimateStatus.UNKNOWN;
        this.estimateCostStatus = CourseEstimateStatus.UNKNOWN; this.dataLimitations = dataLimitations;
    }
    public void addPlace(SavedCoursePlace place) { places.add(place); }
    public void rename(String title) { this.title = title; }
    @PrePersist void create() { createdAt = LocalDateTime.now(); updatedAt = createdAt; }
    @PreUpdate void update() { updatedAt = LocalDateTime.now(); }
    public Long getId(){return id;} public Member getMember(){return member;} public String getTitle(){return title;}
    public String getOriginName(){return originName;} public BigDecimal getOriginLatitude(){return originLatitude;}
    public BigDecimal getOriginLongitude(){return originLongitude;} public int getRequestedPlaceCount(){return requestedPlaceCount;}
    public int getActualPlaceCount(){return actualPlaceCount;} public int getRandomness(){return randomness;}
    public Integer getAvailableMinutes(){return availableMinutes;} public Long getBudget(){return budget;}
    public CourseEstimateStatus getEstimateTimeStatus(){return estimateTimeStatus;}
    public CourseEstimateStatus getEstimateCostStatus(){return estimateCostStatus;}
    public String getDataLimitations(){return dataLimitations;} public List<SavedCoursePlace> getPlaces(){return places;}
    public LocalDateTime getCreatedAt(){return createdAt;} public LocalDateTime getUpdatedAt(){return updatedAt;}
    public Long getVersion(){return version;}
}
