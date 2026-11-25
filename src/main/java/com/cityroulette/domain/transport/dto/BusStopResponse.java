package com.cityroulette.domain.transport.dto;
import java.math.BigDecimal;
public record BusStopResponse(Long id,String externalStopId,String arsNumber,String name,BigDecimal latitude,BigDecimal longitude,String stopType) { }
