package com.cityroulette.domain;

import com.cityroulette.dto.recommendation.RecommendationTransitStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * 저장 코스의 방문 지점의 저장 구조를 표현하는 JPA 엔티티입니다. 식별자와 컬럼 매핑을 정의합니다.
 */
@Entity
@Table(name = "saved_course_places", uniqueConstraints = {
        @UniqueConstraint(name = "uk_saved_course_visit_order", columnNames = {"saved_course_id", "visit_order"}),
        @UniqueConstraint(name = "uk_saved_course_place", columnNames = {"saved_course_id", "place_id"})})
public class SavedCoursePlace {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "saved_course_id", nullable = false) private SavedCourse savedCourse;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "place_id") private Place place;
    @Column(name = "visit_order", nullable = false) private int visitOrder;
    @Column(name = "place_name_snapshot", nullable = false, length = 255) private String placeNameSnapshot;
    @Column(name = "category_snapshot", length = 100) private String categorySnapshot;
    @Column(name = "address_snapshot", length = 500) private String addressSnapshot;
    @Column(name = "latitude_snapshot", nullable = false, precision = 10, scale = 7) private BigDecimal latitudeSnapshot;
    @Column(name = "longitude_snapshot", nullable = false, precision = 10, scale = 7) private BigDecimal longitudeSnapshot;
    @Column(name = "recommendation_score", nullable = false, precision = 6, scale = 2) private BigDecimal recommendationScore;
    @Column(name = "recommendation_reason", length = 5000) private String recommendationReason;
    @Column(name = "distance_from_previous_meters", nullable = false, precision = 12, scale = 1) private BigDecimal distanceFromPreviousMeters;
    @Enumerated(EnumType.STRING) @Column(name = "transit_verification_status", nullable = false, length = 30) private RecommendationTransitStatus transitVerificationStatus;
    @Column(name = "data_limitations", length = 5000) private String dataLimitations;

    protected SavedCoursePlace() { }
    public SavedCoursePlace(SavedCourse savedCourse, Place place, int visitOrder, String placeNameSnapshot,
            String categorySnapshot, String addressSnapshot, BigDecimal latitudeSnapshot, BigDecimal longitudeSnapshot,
            BigDecimal recommendationScore, String recommendationReason, BigDecimal distanceFromPreviousMeters,
            RecommendationTransitStatus transitVerificationStatus, String dataLimitations) {
        this.savedCourse=savedCourse;this.place=place;this.visitOrder=visitOrder;this.placeNameSnapshot=placeNameSnapshot;
        this.categorySnapshot=categorySnapshot;this.addressSnapshot=addressSnapshot;this.latitudeSnapshot=latitudeSnapshot;
        this.longitudeSnapshot=longitudeSnapshot;this.recommendationScore=recommendationScore;
        this.recommendationReason=recommendationReason;this.distanceFromPreviousMeters=distanceFromPreviousMeters;
        this.transitVerificationStatus=transitVerificationStatus;this.dataLimitations=dataLimitations;
    }
    public Long getId(){return id;} public SavedCourse getSavedCourse(){return savedCourse;} public Place getPlace(){return place;}
    public int getVisitOrder(){return visitOrder;} public String getPlaceNameSnapshot(){return placeNameSnapshot;}
    public String getCategorySnapshot(){return categorySnapshot;} public String getAddressSnapshot(){return addressSnapshot;}
    public BigDecimal getLatitudeSnapshot(){return latitudeSnapshot;} public BigDecimal getLongitudeSnapshot(){return longitudeSnapshot;}
    public BigDecimal getRecommendationScore(){return recommendationScore;} public String getRecommendationReason(){return recommendationReason;}
    public BigDecimal getDistanceFromPreviousMeters(){return distanceFromPreviousMeters;}
    public RecommendationTransitStatus getTransitVerificationStatus(){return transitVerificationStatus;}
    public String getDataLimitations(){return dataLimitations;}
}
