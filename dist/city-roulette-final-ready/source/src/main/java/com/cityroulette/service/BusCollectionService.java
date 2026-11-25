package com.cityroulette.service;

import com.cityroulette.domain.ImportRun;
import com.cityroulette.dto.bus.BusImportReport;
import com.cityroulette.dto.bus.NormalizedBusRoute;
import com.cityroulette.dto.bus.NormalizedBusRouteStop;
import com.cityroulette.dto.bus.NormalizedBusStop;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * 부산 버스 API를 페이지 단위로 호출하고 정규화한 결과를 저장 서비스로 전달한다.
 * 실제 INSERT/UPDATE 판단은 {@link BusImportService}가 담당한다.
 */
@Service
public class BusCollectionService {

    private final BusanBusClient client;
    private final BusStopMapper stopMapper;
    private final BusRouteMapper routeMapper;
    private final BusRouteStopMapper routeStopMapper;
    private final BusImportService importer;
    private final ImportRunService runs;

    public BusCollectionService(
            BusanBusClient client,
            BusStopMapper stopMapper,
            BusRouteMapper routeMapper,
            BusRouteStopMapper routeStopMapper,
            BusImportService importer,
            ImportRunService runs) {
        this.client = client;
        this.stopMapper = stopMapper;
        this.routeMapper = routeMapper;
        this.routeStopMapper = routeStopMapper;
        this.importer = importer;
        this.runs = runs;
    }

    /** 지정한 최대 건수까지만 정류장을 수집한다. */
    public BusImportReport collectStops(boolean dryRun, int pageSize, int limit) {
        validate(pageSize, limit);
        ImportRun run = dryRun ? null : runs.start("BUSAN_BUS_STOP", limit);
        BusImportReport total = BusImportReport.empty("BUSAN_BUS_STOP", dryRun);

        try {
            int page = 1;
            int processed = 0;
            while (processed < limit) {
                int requestSize = Math.min(pageSize, limit - processed);
                var response = client.getStops(page, requestSize, null, null);
                List<NormalizedBusStop> mapped = new ArrayList<>();
                int mappingFailures = 0;

                // 잘못된 원천 행 하나 때문에 정상 정류장까지 버리지 않도록 건별로 변환한다.
                for (var raw : response.items()) {
                    try {
                        mapped.add(stopMapper.map(raw));
                    } catch (RuntimeException exception) {
                        mappingFailures++;
                    }
                }

                BusImportReport report = importer.importStops(mapped, dryRun);
                total = total.add(
                        report.fetched(), report.inserted(), report.updated(), report.skipped(),
                        report.failed() + mappingFailures, 0);
                recordPage(run, report, mappingFailures);

                processed += response.items().size();
                if (response.items().isEmpty() || processed >= response.totalCount()) {
                    break;
                }
                page++;
            }

            complete(run);
            return total;
        } catch (RuntimeException exception) {
            fail(run, exception);
            throw exception;
        }
    }

    /**
     * 검색형 API 응답에서 요청한 번호와 정확히 같은 노선만 골라 정차 순서와 함께 수집한다.
     */
    public BusImportReport collectRoutes(
            boolean dryRun,
            List<String> routeNumbers,
            int routeLimit,
            int stopLimit) {
        if (routeNumbers == null || routeNumbers.isEmpty()) {
            throw new IllegalArgumentException("노선 수집에는 route-numbers가 필요합니다.");
        }

        ImportRun run = dryRun ? null : runs.start("BUSAN_BUS_ROUTE", routeLimit);
        BusImportReport total = BusImportReport.empty("BUSAN_BUS_ROUTE", dryRun);
        Set<String> seenRouteIds = new HashSet<>();

        try {
            routeSearch:
            for (String requestedNumber : routeNumbers) {
                for (var raw : client.getRoutes(requestedNumber)) {
                    if (!isExactRouteNumber(raw.buslinenum(), requestedNumber)) {
                        continue;
                    }
                    if (total.fetched() >= routeLimit) {
                        break routeSearch;
                    }

                    NormalizedBusRoute route;
                    try {
                        route = routeMapper.map(raw);
                    } catch (RuntimeException exception) {
                        total = total.add(1, 0, 0, 0, 1, 0);
                        continue;
                    }

                    if (!seenRouteIds.add(route.externalRouteId())) {
                        total = total.add(1, 0, 0, 1, 0, 0);
                        continue;
                    }

                    List<NormalizedBusRouteStop> links = new ArrayList<>();
                    int mappingFailures = 0;
                    var rawStops = client.getRouteStops(route.externalRouteId())
                            .stream()
                            .limit(stopLimit)
                            .toList();
                    for (var rawStop : rawStops) {
                        try {
                            links.add(routeStopMapper.map(rawStop));
                        } catch (RuntimeException exception) {
                            mappingFailures++;
                        }
                    }

                    BusImportReport report = importer.importRoute(route, links, dryRun);
                    total = total.add(
                            report.fetched(), report.inserted(), report.updated(), report.skipped(),
                            report.failed() + mappingFailures, report.routeStops());
                    recordPage(run, report, mappingFailures);
                }
            }

            complete(run);
            return total;
        } catch (RuntimeException exception) {
            fail(run, exception);
            throw exception;
        }
    }

    private boolean isExactRouteNumber(String actual, String requested) {
        return actual != null && actual.trim().equalsIgnoreCase(requested.trim());
    }

    private void recordPage(ImportRun run, BusImportReport report, int mappingFailures) {
        if (run == null) {
            return;
        }
        runs.addPage(
                run.getId(), report.fetched(), report.inserted(), report.updated(), 0,
                report.skipped(), report.failed() + mappingFailures);
    }

    private void complete(ImportRun run) {
        if (run != null) {
            runs.succeedOnly(run.getId());
        }
    }

    private void fail(ImportRun run, RuntimeException exception) {
        if (run != null) {
            runs.fail(run.getId(), safe(exception));
        }
    }

    private void validate(int pageSize, int limit) {
        if (pageSize < 1 || pageSize > 100 || limit < 1) {
            throw new IllegalArgumentException("page-size는 1~100, limit은 1 이상이어야 합니다.");
        }
    }

    /** 예외 전체 대신 민감정보가 없는 요약 메시지만 실행 이력에 남긴다. */
    private String safe(RuntimeException exception) {
        return exception.getMessage() == null
                ? exception.getClass().getSimpleName()
                : exception.getMessage();
    }
}
