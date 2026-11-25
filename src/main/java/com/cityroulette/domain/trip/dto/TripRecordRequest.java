package com.cityroulette.domain.trip.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

/**
 * 호출자가 보내는 입력의 계약입니다. 값은 선언된 필드와 검증 규칙에 따라 해석합니다.
 */
public class TripRecordRequest {

    @NotBlank(message = "여행 제목을 입력해주세요.")
    @Size(max = 100, message = "여행 제목은 100자 이하로 입력해주세요.")
    private String title;

    @NotBlank(message = "방문 장소를 입력해주세요.")
    @Size(max = 200, message = "방문 장소는 200자 이하로 입력해주세요.")
    private String placeName;

    @Size(max = 255, message = "장소 주소는 255자 이하로 입력해주세요.")
    private String placeAddress;

    @Size(max = 2000, message = "메모는 2000자 이하로 입력해주세요.")
    private String memo;

    @NotNull(message = "방문일을 선택해주세요.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate visitDate;

    private MultipartFile image;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getPlaceName() { return placeName; }
    public void setPlaceName(String placeName) { this.placeName = placeName; }
    public String getPlaceAddress() { return placeAddress; }
    public void setPlaceAddress(String placeAddress) { this.placeAddress = placeAddress; }
    public String getMemo() { return memo; }
    public void setMemo(String memo) { this.memo = memo; }
    public LocalDate getVisitDate() { return visitDate; }
    public void setVisitDate(LocalDate visitDate) { this.visitDate = visitDate; }
    public MultipartFile getImage() { return image; }
    public void setImage(MultipartFile image) { this.image = image; }
}
