package com.cityroulette.dto.bus;

import java.math.BigDecimal;

public record TransitStopSummary(
        Long id,String externalStopId,String arsNumber,String name,
        BigDecimal latitude,BigDecimal longitude) { }
