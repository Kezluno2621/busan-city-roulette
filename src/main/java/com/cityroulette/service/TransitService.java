package com.cityroulette.service;

import com.cityroulette.dto.transit.OdsayPathDto;
import com.cityroulette.dto.transit.OdsayPathInfoDto;
import com.cityroulette.dto.transit.OdsayResponse;
import com.cityroulette.dto.transit.TransitRouteDto;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class TransitService {

    private final OdsayApiClient odsayApiClient;

    public TransitService(OdsayApiClient odsayApiClient) {
        this.odsayApiClient = odsayApiClient;
    }

    public List<TransitRouteDto> findRoutes(
            double startLongitude,
            double startLatitude,
            double endLongitude,
            double endLatitude) {
        OdsayResponse response = odsayApiClient.searchPublicTransit(
                startLongitude, startLatitude, endLongitude, endLatitude);
        return toRoutes(response);
    }

    public Optional<TransitRouteDto> findFastestRoute(
            double startLongitude,
            double startLatitude,
            double endLongitude,
            double endLatitude) {
        OdsayResponse response = odsayApiClient.searchPublicTransit(
                startLongitude, startLatitude, endLongitude, endLatitude);

        return toRoutes(response).stream()
                .filter(route -> route.totalTime() != null)
                .min(Comparator.comparingInt(TransitRouteDto::totalTime));
    }

    private List<TransitRouteDto> toRoutes(OdsayResponse response) {
        if (response == null || response.result() == null
                || response.result().path() == null) {
            return List.of();
        }

        return response.result().path().stream()
                .filter(Objects::nonNull)
                .map(this::toRoute)
                .toList();
    }

    private TransitRouteDto toRoute(OdsayPathDto path) {
        OdsayPathInfoDto info = path.info();
        if (info == null) {
            return new TransitRouteDto(
                    path.pathType(), null, null, null, null, null, null, null);
        }

        return new TransitRouteDto(
                path.pathType(),
                info.totalTime(),
                info.totalWalk(),
                info.payment(),
                info.busTransitCount(),
                info.subwayTransitCount(),
                info.firstStartStation(),
                info.lastEndStation());
    }
}
