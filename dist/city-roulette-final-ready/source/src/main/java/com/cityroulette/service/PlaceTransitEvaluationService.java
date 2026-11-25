package com.cityroulette.service;

import com.cityroulette.dto.place.NearbyPlaceDto;
import com.cityroulette.dto.place.TransitPlaceCandidateDto;
import com.cityroulette.dto.transit.TransitRouteDto;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class PlaceTransitEvaluationService {

    private static final Comparator<TransitPlaceCandidateDto> TRAVEL_TIME_ORDER =
            Comparator.comparing(
                    TransitPlaceCandidateDto::totalTime,
                    Comparator.nullsLast(Comparator.naturalOrder()));

    private final PlaceCandidateService placeCandidateService;
    private final TransitService transitService;

    public PlaceTransitEvaluationService(
            PlaceCandidateService placeCandidateService,
            TransitService transitService) {
        this.placeCandidateService = placeCandidateService;
        this.transitService = transitService;
    }

    public List<TransitPlaceCandidateDto> evaluateCandidates(
            double startLatitude,
            double startLongitude,
            int limit) {
        List<NearbyPlaceDto> nearbyPlaces = placeCandidateService.findNearestPlaces(
                startLatitude, startLongitude, limit);
        List<TransitPlaceCandidateDto> evaluated = new ArrayList<>(nearbyPlaces.size());

        for (NearbyPlaceDto place : nearbyPlaces) {
            evaluated.add(evaluateOne(place, startLatitude, startLongitude));
        }

        evaluated.sort(TRAVEL_TIME_ORDER);
        return List.copyOf(evaluated);
    }

    private TransitPlaceCandidateDto evaluateOne(
            NearbyPlaceDto place,
            double startLatitude,
            double startLongitude) {
        try {
            // TransitService는 ODsay 규격에 맞게 경도(X), 위도(Y) 순서로 받는다.
            Optional<TransitRouteDto> route = transitService.findFastestRoute(
                    startLongitude,
                    startLatitude,
                    place.longitude(),
                    place.latitude());

            return route.map(value -> availableCandidate(place, value))
                    .orElseGet(() -> unavailableCandidate(place));
        } catch (OdsayApiException exception) {
            if (exception.isRouteNotFound()) {
                return unavailableCandidate(place);
            }
            throw exception;
        }
    }

    private TransitPlaceCandidateDto availableCandidate(
            NearbyPlaceDto place,
            TransitRouteDto route) {
        return new TransitPlaceCandidateDto(
                place.id(),
                place.externalId(),
                place.name(),
                place.category(),
                place.address(),
                place.latitude(),
                place.longitude(),
                place.distanceKm(),
                route.totalTime(),
                route.totalWalk(),
                route.payment(),
                route.busTransitCount(),
                route.subwayTransitCount(),
                route.firstStartStation(),
                route.lastEndStation(),
                route.pathType(),
                true);
    }

    private TransitPlaceCandidateDto unavailableCandidate(NearbyPlaceDto place) {
        return new TransitPlaceCandidateDto(
                place.id(),
                place.externalId(),
                place.name(),
                place.category(),
                place.address(),
                place.latitude(),
                place.longitude(),
                place.distanceKm(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                false);
    }
}
