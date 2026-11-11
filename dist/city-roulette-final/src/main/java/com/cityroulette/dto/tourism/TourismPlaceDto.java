package com.cityroulette.dto.tourism;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TourismPlaceDto(
        String contentid,
        String contenttypeid,
        String title,
        String addr1,
        String addr2,
        String mapx,
        String mapy,
        String firstimage,
        String firstimage2,
        String cpyrhtDivCd,
        String lDongRegnCd,
        String lDongSignguCd,
        String lclsSystm1,
        String lclsSystm2,
        String lclsSystm3,
        String tel,
        String zipcode,
        String createdtime,
        String modifiedtime
) {
}
