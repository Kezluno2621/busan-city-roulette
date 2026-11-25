package com.cityroulette.domain.transport.dto;
public record BusRouteResponse(Long id,String externalRouteId,String routeNumber,String routeType,String startStopName,String endStopName) { }
