package com.cityroulette.dto.transit;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OdsayPathDto(Integer pathType, OdsayPathInfoDto info) {
}
