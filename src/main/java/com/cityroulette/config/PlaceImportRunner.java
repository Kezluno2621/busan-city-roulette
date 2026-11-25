package com.cityroulette.config;

import com.cityroulette.dto.importing.PlaceImportResult;
import com.cityroulette.service.PlaceCollectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/** 설정한 공급자의 장소 수집과 적재를 실행합니다. */
@Component
@ConditionalOnProperty(name = "place.import.enabled", havingValue = "true")
public class PlaceImportRunner implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(PlaceImportRunner.class);
    private final PlaceCollectionService collection;
    private final String configuredProviders;
    private final boolean dryRun;
    private final int pageSize;
    private final int limit;

    public PlaceImportRunner(PlaceCollectionService collection,
                             @Value("${place.import.providers:ALL}") String configuredProviders,
                             @Value("${place.import.dry-run:true}") boolean dryRun,
                             @Value("${place.import.page-size:10}") int pageSize,
                             @Value("${place.import.limit:10}") int limit) {
        this.collection=collection; this.configuredProviders=configuredProviders;
        this.dryRun=dryRun; this.pageSize=pageSize; this.limit=limit;
    }

    @Override public void run(ApplicationArguments args) {
        List<String> names = "ALL".equalsIgnoreCase(configuredProviders.trim())
                ? collection.providerNames()
                : Arrays.stream(configuredProviders.split(",")).map(String::trim).filter(s -> !s.isBlank()).toList();
        for (String name : names) {
            try {
                PlaceImportResult result=collection.collect(name,dryRun,pageSize,limit);
                log.info("PLACE_IMPORT_RESULT provider={} dryRun={} fetched={} inserted={} updated={} duplicates={} skipped={} failed={}",
                        result.provider(),result.dryRun(),result.fetched(),result.inserted(),result.updated(),
                        result.duplicates(),result.skipped(),result.failed());
            } catch (RuntimeException exception) {
                log.error("PLACE_IMPORT_FAILED provider={} type={}", name, exception.getClass().getSimpleName());
            }
        }
    }
}
