package com.cityroulette.integration.kakao.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KakaoPlaceSearchResponse(List<KakaoPlaceDocument> documents) {
}
