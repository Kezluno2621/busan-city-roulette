package com.cityroulette.domain.transport.dto;

import java.math.BigDecimal;

public record TransitStopSummary(
        Long id,String externalStopId,String arsNumber,String name,
        BigDecimal latitude,BigDecimal longitude) { }
