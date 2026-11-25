package com.cityroulette.integration.tourapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * TourAPI의 항목 응답을 JSON 구조대로 받습니다. 항목 목록을 읽기 전에 공급자 결과를 확인할 때 사용합니다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TourismApiResponse(TourismResponse response) {
}
