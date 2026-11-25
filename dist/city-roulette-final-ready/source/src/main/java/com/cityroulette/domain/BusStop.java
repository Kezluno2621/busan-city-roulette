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
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 부산 버스 정류소의 기준 정보를 저장하는 JPA 엔티티입니다.
 *
 * <p>{@code externalStopId}는 부산버스 API의 공식 정류소 식별자이며 ARS 번호와는
 * 다른 값입니다. 노선 정차 관계는 {@link BusRouteStop}이 이 엔티티를 참조합니다.</p>
 */
@Entity
@Table(
        name = "bus_stops",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_bus_stops_external_id",
                columnNames = "external_stop_id"))
public class BusStop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_stop_id", nullable = false, length = 30)
    private String externalStopId;

    @Column(name = "ars_number", length = 20)
    private String arsNumber;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "stop_type", length = 50)
    private String stopType;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected BusStop() {
        // JPA가 리플렉션으로 엔티티를 생성할 때 사용합니다.
    }

    public BusStop(
            String externalStopId,
            String arsNumber,
            String name,
            BigDecimal latitude,
            BigDecimal longitude,
            String stopType) {
        this.externalStopId = externalStopId;
        update(arsNumber, name, latitude, longitude, stopType);
    }

    /** API 재수집 결과로 변경 가능한 정류소 정보를 갱신합니다. */
    public void update(
            String arsNumber,
            String name,
            BigDecimal latitude,
            BigDecimal longitude,
            String stopType) {
        this.arsNumber = arsNumber;
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.stopType = stopType;
    }

    /**
     * 공식 식별자로 일치가 확인된 정류소에 좌표를 보강합니다.
     *
     * @throws IllegalArgumentException 위도 또는 경도가 누락된 경우
     */
    public void enrichCoordinates(
            BigDecimal latitude,
            BigDecimal longitude,
            String arsNumber,
            String stopType) {
        if (latitude == null || longitude == null) {
            throw new IllegalArgumentException("좌표 보강에는 위도와 경도가 모두 필요합니다.");
        }
        this.latitude = latitude;
        this.longitude = longitude;
        if (this.arsNumber == null && arsNumber != null) {
            this.arsNumber = arsNumber;
        }
        if (this.stopType == null && stopType != null) {
            this.stopType = stopType;
        }
    }

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getExternalStopId() {
        return externalStopId;
    }

    public String getArsNumber() {
        return arsNumber;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public String getStopType() {
        return stopType;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
