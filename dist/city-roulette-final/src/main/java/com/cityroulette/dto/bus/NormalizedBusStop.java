package com.cityroulette.dto.bus;
import java.math.BigDecimal;
public record NormalizedBusStop(String externalStopId,String arsNumber,String name,BigDecimal latitude,BigDecimal longitude,String stopType) { }
