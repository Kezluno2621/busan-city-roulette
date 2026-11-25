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
import java.time.LocalDateTime;

/**
 * 부산버스 API의 공식 노선 ID를 기준으로 저장한 버스 노선이다.
 * 노선 번호는 사용자 표시값일 뿐 유일 식별자로 사용하지 않는다.
 */
@Entity
@Table(
        name = "bus_routes",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_bus_routes_external_id",
                columnNames = "external_route_id"
        )
)
public class BusRoute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_route_id", nullable = false, length = 30)
    private String externalRouteId;

    @Column(name = "route_number", nullable = false, length = 50)
    private String routeNumber;

    @Column(name = "route_type", length = 50)
    private String routeType;

    @Column(name = "start_stop_name", length = 255)
    private String startStopName;

    @Column(name = "end_stop_name", length = 255)
    private String endStopName;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected BusRoute() {
    }

    public BusRoute(
            String externalRouteId,
            String routeNumber,
            String routeType,
            String startStopName,
            String endStopName
    ) {
        this.externalRouteId = externalRouteId;
        update(routeNumber, routeType, startStopName, endStopName);
    }

    /** 재수집한 최신 노선 표시 정보를 반영하되 공식 외부 ID는 유지한다. */
    public void update(String routeNumber, String routeType, String startStopName, String endStopName) {
        this.routeNumber = routeNumber;
        this.routeType = routeType;
        this.startStopName = startStopName;
        this.endStopName = endStopName;
    }

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getExternalRouteId() {
        return externalRouteId;
    }

    public String getRouteNumber() {
        return routeNumber;
    }

    public String getRouteType() {
        return routeType;
    }

    public String getStartStopName() {
        return startStopName;
    }

    public String getEndStopName() {
        return endStopName;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
