package com.cityroulette.config;

import com.cityroulette.dto.bus.BusImportReport;
import com.cityroulette.service.BusCollectionService;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 명시적으로 {@code bus.import.enabled=true}일 때만 실행되는 수동 버스 수집 진입점입니다.
 *
 * <p>웹 요청 처리와 분리되어 있으며, 기본값은 dry-run과 소량 수집입니다.</p>
 */
@Component
@ConditionalOnProperty(name = "bus.import.enabled", havingValue = "true")
public class BusImportRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BusImportRunner.class);

    private final BusCollectionService service;
    private final String mode;
    private final boolean dryRun;
    private final int pageSize;
    private final int limit;
    private final int routeLimit;
    private final int routeStopLimit;
    private final List<String> routeNumbers;

    public BusImportRunner(
            BusCollectionService service,
            @Value("${bus.import.mode:STOPS}") String mode,
            @Value("${bus.import.dry-run:true}") boolean dryRun,
            @Value("${bus.import.page-size:10}") int pageSize,
            @Value("${bus.import.limit:10}") int limit,
            @Value("${bus.import.route-limit:1}") int routeLimit,
            @Value("${bus.import.route-stop-limit:20}") int routeStopLimit,
            @Value("${bus.import.route-numbers:}") String routeNumbers) {
        this.service = service;
        this.mode = mode.toUpperCase(Locale.ROOT);
        this.dryRun = dryRun;
        this.pageSize = pageSize;
        this.limit = limit;
        this.routeLimit = routeLimit;
        this.routeStopLimit = routeStopLimit;
        this.routeNumbers = Arrays.stream(routeNumbers.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
    }

    @Override
    public void run(ApplicationArguments args) {
        if (mode.equals("STOPS") || mode.equals("ALL")) {
            print(service.collectStops(dryRun, pageSize, limit));
        }
        if (mode.equals("ROUTES") || mode.equals("ALL")) {
            print(service.collectRoutes(dryRun, routeNumbers, routeLimit, routeStopLimit));
        }
    }

    private void print(BusImportReport report) {
        log.info(
                "BUS_IMPORT_RESULT provider={} dryRun={} fetched={} inserted={} updated={} "
                        + "skipped={} failed={} routeStops={}",
                report.provider(),
                report.dryRun(),
                report.fetched(),
                report.inserted(),
                report.updated(),
                report.skipped(),
                report.failed(),
                report.routeStops());
    }
}
