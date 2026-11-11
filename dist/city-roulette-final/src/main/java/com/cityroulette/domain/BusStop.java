package com.cityroulette.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name="bus_stops", uniqueConstraints=@UniqueConstraint(name="uk_bus_stops_external_id", columnNames="external_stop_id"))
public class BusStop {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="external_stop_id",nullable=false,length=30) private String externalStopId;
    @Column(name="ars_number",length=20) private String arsNumber;
    @Column(nullable=false,length=255) private String name;
    @Column(precision=10,scale=7) private BigDecimal latitude;
    @Column(precision=10,scale=7) private BigDecimal longitude;
    @Column(name="stop_type",length=50) private String stopType;
    @Column(name="updated_at",nullable=false) private LocalDateTime updatedAt;
    protected BusStop() { }
    public BusStop(String externalStopId,String arsNumber,String name,BigDecimal latitude,BigDecimal longitude,String stopType) {
        this.externalStopId=externalStopId; update(arsNumber,name,latitude,longitude,stopType);
    }
    public void update(String arsNumber,String name,BigDecimal latitude,BigDecimal longitude,String stopType) {
        this.arsNumber=arsNumber; this.name=name; this.latitude=latitude; this.longitude=longitude; this.stopType=stopType;
    }
    public void enrichCoordinates(BigDecimal latitude,BigDecimal longitude,String arsNumber,String stopType) {
        if(latitude==null||longitude==null)throw new IllegalArgumentException("좌표 보강에는 위도와 경도가 모두 필요합니다.");
        this.latitude=latitude;this.longitude=longitude;
        if(this.arsNumber==null&&arsNumber!=null)this.arsNumber=arsNumber;
        if(this.stopType==null&&stopType!=null)this.stopType=stopType;
    }
    @PrePersist @PreUpdate void touch(){ updatedAt=LocalDateTime.now(); }
    public Long getId(){return id;} public String getExternalStopId(){return externalStopId;}
    public String getArsNumber(){return arsNumber;} public String getName(){return name;}
    public BigDecimal getLatitude(){return latitude;} public BigDecimal getLongitude(){return longitude;}
    public String getStopType(){return stopType;} public LocalDateTime getUpdatedAt(){return updatedAt;}
}
