package com.cityroulette.ingestion.bus.dto;

public record BusStopEnrichmentIssue(String externalStopId,String arsNumber,String reason) { }
