package com.cityroulette.integration.tourapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * TourAPI 지역 코드 응답의 최상위 JSON 래퍼를 매핑합니다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TourismRegionResponseEnvelope(TourismHeader header, TourismRegionBody body) { }
