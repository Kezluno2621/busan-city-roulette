package com.cityroulette.integration.tourapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * TourAPI 응답의 공급자 결과 정보를 담습니다. HTTP 응답 상태와 별도로 확인합니다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TourismHeader(String resultCode, String resultMsg) {
}
