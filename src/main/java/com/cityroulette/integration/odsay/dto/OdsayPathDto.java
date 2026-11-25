package com.cityroulette.integration.odsay.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OdsayPathDto(Integer pathType, OdsayPathInfoDto info) {
}
