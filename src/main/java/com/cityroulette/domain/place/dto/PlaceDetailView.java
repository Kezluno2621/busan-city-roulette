package com.cityroulette.domain.place.dto;
import java.time.LocalDateTime;
import java.util.List;
public record PlaceDetailView(PlaceResponse place,List<PlaceSourceView> sources,LocalDateTime lastSyncedAt,List<NearbyStopRoutesView> nearbyStops,boolean mapAvailable) { }
