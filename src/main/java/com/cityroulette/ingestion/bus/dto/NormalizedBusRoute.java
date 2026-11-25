package com.cityroulette.ingestion.bus.dto;
public record NormalizedBusRoute(String externalRouteId,String routeNumber,String routeType,String startStopName,String endStopName) { }
