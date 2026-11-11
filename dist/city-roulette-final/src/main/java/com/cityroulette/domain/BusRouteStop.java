package com.cityroulette.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name="bus_route_stops",uniqueConstraints=@UniqueConstraint(name="uk_bus_route_stops_sequence",columnNames={"bus_route_id","stop_sequence"}))
public class BusRouteStop {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="bus_route_id",nullable=false) private BusRoute busRoute;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="bus_stop_id",nullable=false) private BusStop busStop;
    @Column(name="stop_sequence",nullable=false) private int stopSequence;
    @Column(length=30) private String direction;
    @Column(name="updated_at",nullable=false) private LocalDateTime updatedAt;
    protected BusRouteStop() { }
    public BusRouteStop(BusRoute route,BusStop stop,int sequence,String direction){busRoute=route;busStop=stop;stopSequence=sequence;this.direction=direction;}
    public void update(BusStop stop,String direction){busStop=stop;this.direction=direction;}
    @PrePersist @PreUpdate void touch(){updatedAt=LocalDateTime.now();}
    public Long getId(){return id;} public BusRoute getBusRoute(){return busRoute;} public BusStop getBusStop(){return busStop;}
    public int getStopSequence(){return stopSequence;} public String getDirection(){return direction;} public LocalDateTime getUpdatedAt(){return updatedAt;}
}
