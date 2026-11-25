package com.cityroulette.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * 여러 공공데이터 공급자의 정보를 통합해 화면과 추천에 사용하는 대표 장소 엔티티입니다.
 *
 * <p>공급자별 원본 식별자와 저작권 정보는 {@link PlaceSource}가 관리합니다. 정규화 필드는
 * 검색과 중복 후보 판정에 사용되며, 불확실한 후보를 자동 병합하지 않습니다.</p>
 */

@Entity
@Table(
        name = "places",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_places_source_external_id",
                columnNames = {"source", "external_id"}))
public class Place {

    public static final String SOURCE_MANUAL = "MANUAL";
    public static final String SOURCE_KOR_SERVICE = "KOR_SERVICE";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id", length = 255)
    private String externalId;

    @NotBlank
    @Size(max = 255)
    @Column(nullable = false, length = 255)
    private String name;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String category;

    @Size(max = 255)
    @Column(length = 255)
    private String address;

    @Size(max = 50)
    @Column(length = 50)
    private String district;

    @DecimalMin("-90.0")
    @DecimalMax("90.0")
    private Double latitude;

    @DecimalMin("-180.0")
    @DecimalMax("180.0")
    private Double longitude;

    @Size(max = 2000)
    @Column(length = 2000)
    private String description;

    @Column(name = "image_url", length = 1000)
    private String imageUrl;

    @Column(name = "normalized_name", length = 255)
    private String normalizedName;

    @Column(name = "normalized_address", length = 500)
    private String normalizedAddress;

    @Column(name = "merge_status", nullable = false, length = 20)
    private String mergeStatus = "ACTIVE";

    @NotBlank
    @Size(max = 50)
    @Column(nullable = false, length = 50)
    private String source;

    private Boolean indoor;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    protected Place() {
        // JPA가 엔티티를 생성할 때 사용하는 기본 생성자입니다.
    }

    public Place(String name, String category, String address,
                 Double latitude, Double longitude, Boolean indoor) {
        this(SOURCE_MANUAL, null, name, category, address, null,
                latitude, longitude, null, indoor);
    }

    public Place(String externalId, String name, String category, String address,
                 Double latitude, Double longitude, Boolean indoor) {
        this(SOURCE_KOR_SERVICE, externalId, name, category, address, null,
                latitude, longitude, null, indoor);
    }

    public Place(
            String source,
            String externalId,
            String name,
            String category,
            String address,
            String district,
            Double latitude,
            Double longitude,
            String description,
            Boolean indoor) {
        this.source = source;
        this.externalId = externalId;
        this.name = name;
        this.category = category;
        this.address = address;
        this.district = district;
        this.latitude = latitude;
        this.longitude = longitude;
        this.description = description;
        this.indoor = indoor;
    }

    public void updateFromApi(
            String name,
            String category,
            String address,
            String district,
            Double latitude,
            Double longitude,
            String description) {
        this.name = name;
        this.category = category;
        this.address = address;
        this.district = district;
        this.latitude = latitude;
        this.longitude = longitude;
        this.description = description;
    }

    public void updateFromApi(String name, String category, String address, String district,
                              Double latitude, Double longitude, String description, String imageUrl) {
        updateFromApi(name, category, address, district, latitude, longitude, description);
        this.imageUrl = imageUrl;
    }

    public void applyNormalizedValues(String normalizedName, String normalizedAddress) {
        this.normalizedName = normalizedName;
        this.normalizedAddress = normalizedAddress;
    }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getExternalId() {
        return externalId;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public String getAddress() {
        return address;
    }

    public String getDistrict() {
        return district;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public String getDescription() {
        return description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getNormalizedName() {
        return normalizedName;
    }

    public String getNormalizedAddress() {
        return normalizedAddress;
    }

    public String getMergeStatus() {
        return mergeStatus;
    }

    public String getSource() {
        return source;
    }

    public Boolean getIndoor() {
        return indoor;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
