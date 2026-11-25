package com.cityroulette.domain.place.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

/**
 * 공급자별 장소 원천의 저장 구조를 표현하는 JPA 엔티티입니다. 식별자와 컬럼 매핑을 정의합니다.
 */
@Entity
@Table(name = "place_sources", uniqueConstraints = @UniqueConstraint(
        name = "uk_place_sources_provider_external", columnNames = {"provider", "external_id"}))
public class PlaceSource {

    public static final String PROVIDER_TOUR_API = "TOUR_API";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "place_id", nullable = false)
    private Place place;

    @Column(nullable = false, length = 50)
    private String provider;

    @Column(name = "external_id", nullable = false, length = 255)
    private String externalId;

    @Column(name = "source_url", length = 1000)
    private String sourceUrl;

    @Column(name = "source_category", length = 255)
    private String sourceCategory;

    @Column(name = "source_modified_at")
    private LocalDateTime sourceModifiedAt;

    @Column(name = "last_synced_at", nullable = false)
    private LocalDateTime lastSyncedAt;

    @Column(length = 500)
    private String attribution;

    @Column(name = "content_license", length = 255)
    private String contentLicense;

    @Column(name = "image_license", length = 255)
    private String imageLicense;

    @Column(nullable = false)
    private boolean active = true;

    protected PlaceSource() {
    }

    public PlaceSource(Place place, String provider, String externalId) {
        this.place = place;
        this.provider = provider;
        this.externalId = externalId;
    }

    public void synchronize(String sourceUrl, String sourceCategory,
                            LocalDateTime sourceModifiedAt, LocalDateTime syncedAt,
                            String attribution, String contentLicense, String imageLicense) {
        this.sourceUrl = sourceUrl;
        this.sourceCategory = sourceCategory;
        this.sourceModifiedAt = sourceModifiedAt;
        this.lastSyncedAt = syncedAt;
        this.attribution = attribution;
        this.contentLicense = contentLicense;
        this.imageLicense = imageLicense;
        this.active = true;
    }

    public Long getId() { return id; }
    public Place getPlace() { return place; }
    public String getProvider() { return provider; }
    public String getExternalId() { return externalId; }
    public String getSourceUrl() { return sourceUrl; }
    public String getSourceCategory() { return sourceCategory; }
    public LocalDateTime getSourceModifiedAt() { return sourceModifiedAt; }
    public LocalDateTime getLastSyncedAt() { return lastSyncedAt; }
    public String getAttribution() { return attribution; }
    public String getContentLicense() { return contentLicense; }
    public String getImageLicense() { return imageLicense; }
    public boolean isActive() { return active; }
}
