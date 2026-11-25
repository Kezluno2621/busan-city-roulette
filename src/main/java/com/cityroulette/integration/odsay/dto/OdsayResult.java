package com.cityroulette.integration.odsay.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OdsayResult(List<OdsayPathDto> path) {
}
