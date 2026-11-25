package com.cityroulette.config;

import com.cityroulette.dto.tourism.TourismCollectionResult;
import com.cityroulette.service.TourismCollectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** TourAPI 관광 데이터의 명시적 수집 작업을 실행합니다. */
@Component
@ConditionalOnProperty(name = "tour.import.enabled", havingValue = "true")
public class TourismCollectorRunner implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(TourismCollectorRunner.class);
    private final TourismCollectionService service;
    private final boolean dryRun;
    private final int pageSize;
    private final int limit;

    public TourismCollectorRunner(TourismCollectionService service,
            @Value("${tour.import.dry-run:true}") boolean dryRun,
            @Value("${tour.import.page-size:20}") int pageSize,
            @Value("${tour.import.limit:20}") int limit) {
        this.service = service; this.dryRun = dryRun; this.pageSize = pageSize; this.limit = limit;
    }

    @Override
    public void run(ApplicationArguments args) {
        TourismCollectionResult result = service.collect(dryRun, pageSize, limit);
        log.info("TourAPI import completed: runId={}, dryRun={}, pages={}, received={}, inserted={}, updated={}, duplicates={}, skipped={}, failed={}",
                result.runId(), result.dryRun(), result.pagesRequested(), result.received(), result.inserted(),
                result.updated(), result.duplicates(), result.skipped(), result.failed());
    }
}
