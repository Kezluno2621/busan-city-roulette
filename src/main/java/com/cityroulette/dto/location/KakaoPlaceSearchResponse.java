package com.cityroulette.dto.location;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KakaoPlaceSearchResponse(List<KakaoPlaceDocument> documents) {
}
