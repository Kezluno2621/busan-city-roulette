package com.cityroulette.service;

import com.cityroulette.domain.Place;
import com.cityroulette.domain.PlaceMergeCandidate;
import com.cityroulette.domain.PlaceSource;
import com.cityroulette.dto.tourism.TourismImportPageResult;
import com.cityroulette.dto.tourism.TourismPlaceDto;
import com.cityroulette.repository.PlaceMergeCandidateRepository;
import com.cityroulette.repository.PlaceRepository;
import com.cityroulette.repository.PlaceSourceRepository;
import com.cityroulette.util.DistanceCalculator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class TourismPageImporter {
    private static final String DATASET_URL = "https://www.data.go.kr/data/15101578/openapi.do";
    private final TourismPlaceMapper mapper;
    private final PlaceRepository placeRepository;
    private final PlaceSourceRepository sourceRepository;
    private final PlaceMergeCandidateRepository candidateRepository;

    public TourismPageImporter(TourismPlaceMapper mapper, PlaceRepository placeRepository,
                               PlaceSourceRepository sourceRepository,
                               PlaceMergeCandidateRepository candidateRepository) {
        this.mapper = mapper;
        this.placeRepository = placeRepository;
        this.sourceRepository = sourceRepository;
        this.candidateRepository = candidateRepository;
    }

    @Transactional
    public TourismImportPageResult importPage(List<TourismPlaceDto> items, boolean dryRun) {
        int inserted = 0, updated = 0, duplicates = 0, skipped = 0;
        for (TourismPlaceDto dto : items) {
            if (!isValid(dto)) { skipped++; continue; }
            String externalId = dto.contentid().trim();
            PlaceSource existing = sourceRepository.findByProviderAndExternalId(
                    PlaceSource.PROVIDER_TOUR_API, externalId).orElse(null);
            if (existing != null) {
                duplicates++;
                updated++;
                if (!dryRun) synchronize(existing, dto);
                continue;
            }
            Place legacyPlace = placeRepository.findBySourceAndExternalId(
                    Place.SOURCE_KOR_SERVICE, externalId).orElse(null);
            if (legacyPlace != null) {
                duplicates++;
                updated++;
                if (!dryRun) {
                    PlaceSource migrated = new PlaceSource(legacyPlace,
                            PlaceSource.PROVIDER_TOUR_API, externalId);
                    synchronize(migrated, dto);
                    sourceRepository.save(migrated);
                }
                continue;
            }
            inserted++;
            if (!dryRun) insert(dto);
        }
        return new TourismImportPageResult(items.size(), inserted, updated, duplicates, skipped, 0);
    }

    private boolean isValid(TourismPlaceDto dto) {
        return dto != null && mapper.isBusan(dto)
                && dto.contentid() != null && !dto.contentid().isBlank()
                && dto.title() != null && !dto.title().isBlank();
    }

    private void insert(TourismPlaceDto dto) {
        List<Place> possibleMatches = placeRepository.findAll().stream()
                .filter(place -> !Place.SOURCE_KOR_SERVICE.equals(place.getSource()))
                .filter(place -> isSimilar(place, dto)).toList();
        Place place = placeRepository.saveAndFlush(mapper.toPlace(dto));
        PlaceSource source = new PlaceSource(place, PlaceSource.PROVIDER_TOUR_API, dto.contentid().trim());
        synchronize(source, dto);
        sourceRepository.save(source);
        for (Place possibleMatch : possibleMatches) {
            if (!candidateRepository.existsByPlaceIdAndCandidatePlaceId(place.getId(), possibleMatch.getId())) {
                candidateRepository.save(new PlaceMergeCandidate(place, possibleMatch,
                        "정규화된 이름과 주소 또는 100m 이내 좌표가 유사함"));
            }
        }
    }

    private void synchronize(PlaceSource source, TourismPlaceDto dto) {
        Place mapped = mapper.toPlace(dto);
        source.getPlace().updateFromApi(mapped.getName(), mapped.getCategory(), mapped.getAddress(),
                mapped.getDistrict(), mapped.getLatitude(), mapped.getLongitude(), mapped.getDescription(),
                mapped.getImageUrl());
        source.synchronize(DATASET_URL, mapper.sourceCategoryOf(dto), mapper.sourceModifiedAtOf(dto),
                LocalDateTime.now(), "한국관광공사 TourAPI", "공공데이터포털 이용조건 확인",
                mapper.imageLicenseOf(dto));
    }

    private boolean isSimilar(Place place, TourismPlaceDto dto) {
        Place incoming = mapper.toPlace(dto);
        if (!normalize(place.getName()).equals(normalize(incoming.getName()))) return false;
        if (!normalize(place.getAddress()).isEmpty()
                && normalize(place.getAddress()).equals(normalize(incoming.getAddress()))) return true;
        return place.getLatitude() != null && place.getLongitude() != null
                && incoming.getLatitude() != null && incoming.getLongitude() != null
                && DistanceCalculator.calculateKm(place.getLatitude(), place.getLongitude(),
                incoming.getLatitude(), incoming.getLongitude()) <= 0.1;
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[^0-9a-z가-힣]", "");
    }
}
