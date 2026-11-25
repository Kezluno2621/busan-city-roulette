package com.cityroulette.domain.transport.dto;

import java.math.BigDecimal;

public record TransitGraphStop(
        Long stopId,
        String externalStopId,
        String arsNumber,
        String name,
        BigDecimal latitude,
        BigDecimal longitude,
        int sequence,
        String direction) {
    public boolean hasCoordinates(){return latitude!=null&&longitude!=null;}
}
