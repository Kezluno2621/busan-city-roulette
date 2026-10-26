package com.cityroulette.dto.transit;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OdsayResponse(OdsayResult result, JsonNode error) {
}
