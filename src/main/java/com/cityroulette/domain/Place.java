package com.cityroulette.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "places")
public class Place {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id")
    private String externalId;

    @Column(nullable = false)
    private String name;
    private String category;
    private String address;
    private Double latitude;
    private Double longitude;
    private Boolean indoor;

    protected Place() {
        // JPA가 엔티티를 생성할 때 사용하는 기본 생성자입니다.
    }

    public Place(String name, String category, String address,
                 Double latitude, Double longitude, Boolean indoor) {
        this(null, name, category, address, latitude, longitude, indoor);
    }

    public Place(String externalId, String name, String category, String address,
                 Double latitude, Double longitude, Boolean indoor) {
        this.externalId = externalId;
        this.name = name;
        this.category = category;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.indoor = indoor;
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

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public Boolean getIndoor() {
        return indoor;
    }
}
