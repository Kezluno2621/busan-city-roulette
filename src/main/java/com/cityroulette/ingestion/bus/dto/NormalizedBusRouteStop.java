package com.cityroulette.ingestion.bus.dto;

import java.math.BigDecimal;

public record NormalizedBusRouteStop(
        String externalStopId,String arsNumber,String stopName,int sequence,String direction,
        BigDecimal latitude,BigDecimal longitude) {
    public NormalizedBusRouteStop(String externalStopId,String arsNumber,String stopName,int sequence,String direction){
        this(externalStopId,arsNumber,stopName,sequence,direction,null,null);
    }
}
