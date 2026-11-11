package com.cityroulette.dto.bus;
import java.math.BigDecimal;
public record BusStopResponse(Long id,String externalStopId,String arsNumber,String name,BigDecimal latitude,BigDecimal longitude,String stopType) { }
