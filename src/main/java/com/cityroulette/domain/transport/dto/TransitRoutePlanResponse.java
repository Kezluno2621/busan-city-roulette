package com.cityroulette.domain.transport.dto;

import java.util.List;

public record TransitRoutePlanResponse(
        Long originPlaceId,
        Long destinationPlaceId,
        int nearbyRadiusMeters,
        TransitVerificationStatus verificationStatus,
        TransitDataCoverageStatus dataCoverageStatus,
        String interpretation,
        String walkingDistanceType,
        List<TransitRouteCandidate> candidates) { }
