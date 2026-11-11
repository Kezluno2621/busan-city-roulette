package com.cityroulette.service;

import com.cityroulette.domain.Place;
import com.cityroulette.domain.PlaceMergeCandidate;
import com.cityroulette.domain.PlaceSource;
import com.cityroulette.dto.importing.NormalizedPlace;
import com.cityroulette.dto.importing.PlaceImportResult;
import com.cityroulette.repository.PlaceMergeCandidateRepository;
import com.cityroulette.repository.PlaceRepository;
import com.cityroulette.repository.PlaceSourceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;

@Service
public class PlaceImportService {
    private final PlaceRepository places;
    private final PlaceSourceRepository sources;
    private final PlaceMergeCandidateRepository candidates;

    public PlaceImportService(PlaceRepository places, PlaceSourceRepository sources,
                              PlaceMergeCandidateRepository candidates) {
        this.places = places; this.sources = sources; this.candidates = candidates;
    }

    @Transactional
    public PlaceImportResult importItems(String provider, List<NormalizedPlace> items, boolean dryRun) {
        PlaceImportResult result = PlaceImportResult.empty(provider, dryRun);
        var seen = new HashSet<String>();
        for (NormalizedPlace item : items) {
            try {
                if (!valid(provider, item)) { result = result.add(1,0,0,0,1,0); continue; }
                if (!seen.add(item.externalId())) { result = result.add(1,0,0,1,0,0); continue; }
                var existing = sources.findByProviderAndExternalId(provider, item.externalId());
                if (existing.isPresent()) {
                    if (!dryRun) synchronize(existing.get().getPlace(), existing.get(), item);
                    result = result.add(1,0,1,0,0,0);
                } else {
                    if (!dryRun) create(provider, item);
                    result = result.add(1,1,0,0,0,0);
                }
            } catch (RuntimeException exception) {
                result = result.add(1,0,0,0,0,1);
            }
        }
        return result;
    }

    private boolean valid(String provider, NormalizedPlace item) {
        return item != null && item.recordType() == NormalizedPlace.RecordType.PLACE
                && provider.equals(item.provider()) && item.externalId() != null && !item.externalId().isBlank()
                && item.name() != null && !item.name().isBlank() && item.category() != null;
    }

    private void create(String provider, NormalizedPlace item) {
        Place place = new Place(provider, item.externalId(), item.name(), item.category(), item.address(),
                item.district(), item.latitude(), item.longitude(), item.description(), null);
        place.updateFromApi(item.name(), item.category(), item.address(), item.district(), item.latitude(),
                item.longitude(), item.description(), licensedImage(item));
        place.applyNormalizedValues(item.normalizedName(), item.normalizedAddress());
        places.save(place);
        PlaceSource source = new PlaceSource(place, provider, item.externalId());
        syncSource(source, item);
        sources.save(source);
        recordCandidates(place);
    }

    private void synchronize(Place place, PlaceSource source, NormalizedPlace item) {
        place.updateFromApi(item.name(), item.category(), item.address(), item.district(), item.latitude(),
                item.longitude(), item.description(), licensedImage(item));
        place.applyNormalizedValues(item.normalizedName(), item.normalizedAddress());
        syncSource(source, item);
    }

    private void syncSource(PlaceSource source, NormalizedPlace item) {
        source.synchronize(item.sourceUrl(), item.sourceCategory(), item.sourceModifiedAt(), LocalDateTime.now(),
                item.attribution(), item.contentLicense(), item.imageLicense());
    }

    private String licensedImage(NormalizedPlace item) {
        return item.imageLicense() == null || item.imageLicense().isBlank() ? null : item.imageUrl();
    }

    private void recordCandidates(Place place) {
        if (place.getNormalizedName() == null) return;
        for (Place other : places.findByNormalizedNameAndIdNot(place.getNormalizedName(), place.getId())) {
            String reason = matchReason(place, other);
            if (reason != null && !candidates.existsByPlaceIdAndCandidatePlaceId(place.getId(), other.getId())) {
                candidates.save(new PlaceMergeCandidate(place, other, reason));
            }
        }
    }

    private String matchReason(Place one, Place two) {
        if (one.getNormalizedAddress() != null && one.getNormalizedAddress().equals(two.getNormalizedAddress()))
            return "normalized name and address match";
        if (distanceMeters(one, two) <= 100.0) return "normalized name match and coordinates within 100m";
        return null;
    }

    private double distanceMeters(Place one, Place two) {
        if (one.getLatitude() == null || one.getLongitude() == null || two.getLatitude() == null || two.getLongitude() == null)
            return Double.POSITIVE_INFINITY;
        double lat1=Math.toRadians(one.getLatitude()), lat2=Math.toRadians(two.getLatitude());
        double dLat=lat2-lat1, dLon=Math.toRadians(two.getLongitude()-one.getLongitude());
        double a=Math.sin(dLat/2)*Math.sin(dLat/2)+Math.cos(lat1)*Math.cos(lat2)*Math.sin(dLon/2)*Math.sin(dLon/2);
        return 6371000.0 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
    }
}
