package com.cityroulette.config;

import com.cityroulette.repository.PlaceRepository;
import com.cityroulette.service.PlaceDataQualityService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

/** 적재된 장소 데이터의 품질 지표를 조사합니다. */
@Component
@ConditionalOnProperty(name = "place.quality.enabled", havingValue = "true")
public class PlaceDataQualityRunner implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(PlaceDataQualityRunner.class);
    private final PlaceDataQualityService quality;
    private final PlaceRepository places;

    public PlaceDataQualityRunner(PlaceDataQualityService quality, PlaceRepository places) {
        this.quality = quality;
        this.places = places;
    }

    @Override
    public void run(ApplicationArguments args) {
        long reportStart = System.nanoTime();
        var report = quality.measure();
        long reportMillis = elapsed(reportStart);
        long listStart = System.nanoTime();
        var firstPage = places.search(null, null, null, PageRequest.of(0, 20));
        long listMillis = elapsed(listStart);
        log.info("PLACE_QUALITY total={} missingCoordinates={} coordinateMissingRate={} missingAddresses={} addressMissingRate={} missingImages={} imageMissingRate={} mergeCandidates={}",
                report.totalPlaces(), report.missingCoordinates(), report.missingCoordinateRate(),
                report.missingAddresses(), report.missingAddressRate(), report.missingImages(),
                report.missingImageRate(), report.mergeCandidates());
        log.info("PLACE_QUALITY providers={} categories={} districts={}",
                report.providers(), report.categoryDistribution(), report.districtDistribution());
        log.info("PLACE_QUALITY_PERFORMANCE reportMillis={} firstPageMillis={} pageElements={} totalElements={}",
                reportMillis, listMillis, firstPage.getNumberOfElements(), firstPage.getTotalElements());
    }

    private long elapsed(long started) { return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started); }
}
