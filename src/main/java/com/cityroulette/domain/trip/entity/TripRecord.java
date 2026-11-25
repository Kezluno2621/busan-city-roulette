package com.cityroulette.domain.trip.entity;

import com.cityroulette.domain.member.entity.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 회원 여행 기록의 저장 구조를 표현하는 JPA 엔티티입니다. 식별자와 컬럼 매핑을 정의합니다.
 */
@Entity
@Table(name = "trip_records")
public class TripRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 200)
    private String placeName;

    @Column(length = 255)
    private String placeAddress;

    @Column(length = 2000)
    private String memo;

    @Column(nullable = false)
    private LocalDate visitDate;

    @Column(length = 255)
    private String originalFileName;

    @Column(length = 255)
    private String storedFileName;

    @Column(length = 100)
    private String fileContentType;

    private Long fileSize;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected TripRecord() {
        // JPA용 기본 생성자입니다.
    }

    public TripRecord(
            Member member,
            String title,
            String placeName,
            String placeAddress,
            String memo,
            LocalDate visitDate,
            String originalFileName,
            String storedFileName,
            String fileContentType,
            Long fileSize) {
        this.member = member;
        this.title = title;
        this.placeName = placeName;
        this.placeAddress = placeAddress;
        this.memo = memo;
        this.visitDate = visitDate;
        this.originalFileName = originalFileName;
        this.storedFileName = storedFileName;
        this.fileContentType = fileContentType;
        this.fileSize = fileSize;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Member getMember() { return member; }
    public String getTitle() { return title; }
    public String getPlaceName() { return placeName; }
    public String getPlaceAddress() { return placeAddress; }
    public String getMemo() { return memo; }
    public LocalDate getVisitDate() { return visitDate; }
    public String getOriginalFileName() { return originalFileName; }
    public String getStoredFileName() { return storedFileName; }
    public String getFileContentType() { return fileContentType; }
    public Long getFileSize() { return fileSize; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public boolean hasAttachment() {
        return storedFileName != null && !storedFileName.isBlank();
    }
}
