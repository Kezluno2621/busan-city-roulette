package com.cityroulette.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

/**
 * 한 노선이 정류장을 방문하는 순서를 표현하는 연결 엔티티다.
 * 경로 탐색은 이 엔티티의 순서와 방향을 보존해 반대 방향 후보를 배제한다.
 */
@Entity
@Table(
        name = "bus_route_stops",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_bus_route_stops_sequence",
                columnNames = {"bus_route_id", "stop_sequence"}
        )
)
public class BusRouteStop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bus_route_id", nullable = false)
    private BusRoute busRoute;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bus_stop_id", nullable = false)
    private BusStop busStop;

    @Column(name = "stop_sequence", nullable = false)
    private int stopSequence;

    @Column(length = 30)
    private String direction;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected BusRouteStop() {
    }

    public BusRouteStop(BusRoute route, BusStop stop, int sequence, String direction) {
        this.busRoute = route;
        this.busStop = stop;
        this.stopSequence = sequence;
        this.direction = direction;
    }

    /** 좌표 보강 등으로 정류장 연결이 확정되었을 때 관계 정보를 갱신한다. */
    public void update(BusStop stop, String direction) {
        this.busStop = stop;
        this.direction = direction;
    }

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public BusRoute getBusRoute() {
        return busRoute;
    }

    public BusStop getBusStop() {
        return busStop;
    }

    public int getStopSequence() {
        return stopSequence;
    }

    public String getDirection() {
        return direction;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
