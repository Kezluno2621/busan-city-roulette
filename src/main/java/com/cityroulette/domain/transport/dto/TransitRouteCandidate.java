package com.cityroulette.domain.transport.dto;

import java.util.List;

public record TransitRouteCandidate(
        Long originPlaceId,
        Long destinationPlaceId,
        TransitRouteType routeType,
        int transferCount,
        TransitStopSummary boardingStop,
        TransitStopSummary alightingStop,
        TransitStopSummary transferStop,
        List<String> busRouteNumbers,
        List<String> busRouteExternalIds,
        int stopCount,
        double walkingDistanceMeters,
        TransitVerificationStatus verificationStatus,
        TransitDataCoverageStatus dataCoverageStatus) { }
