package com.cityroulette.service;

import com.cityroulette.domain.BusRoute;
import com.cityroulette.domain.BusRouteStop;
import com.cityroulette.domain.BusStop;
import com.cityroulette.dto.bus.BusImportReport;
import com.cityroulette.dto.bus.NormalizedBusRoute;
import com.cityroulette.dto.bus.NormalizedBusRouteStop;
import com.cityroulette.dto.bus.NormalizedBusStop;
import com.cityroulette.repository.BusRouteRepository;
import com.cityroulette.repository.BusRouteStopRepository;
import com.cityroulette.repository.BusStopRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 정규화된 버스 데이터를 외부 식별자를 기준으로 신규 저장하거나 갱신한다.
 * 같은 데이터를 재수집해도 행이 늘어나지 않도록 기존 레코드를 먼저 조회한다.
 */
@Service
public class BusImportService {

    private static final String STOP_PROVIDER = "BUSAN_BUS_STOP";
    private static final String ROUTE_PROVIDER = "BUSAN_BUS_ROUTE";

    private final BusStopRepository stops;
    private final BusRouteRepository routes;
    private final BusRouteStopRepository routeStops;

    public BusImportService(
            BusStopRepository stops,
            BusRouteRepository routes,
            BusRouteStopRepository routeStops) {
        this.stops = stops;
        this.routes = routes;
        this.routeStops = routeStops;
    }

    /** 정류장 목록을 외부 정류장 ID 기준으로 멱등 저장한다. */
    @Transactional
    public BusImportReport importStops(List<NormalizedBusStop> items, boolean dryRun) {
        BusImportReport result = BusImportReport.empty(STOP_PROVIDER, dryRun);
        Set<String> seenExternalIds = new HashSet<>();

        for (NormalizedBusStop item : items) {
            try {
                if (!isValidStop(item) || !seenExternalIds.add(item.externalStopId())) {
                    result = result.add(1, 0, 0, 1, 0, 0);
                    continue;
                }

                var existing = stops.findByExternalStopId(item.externalStopId());
                if (existing.isPresent()) {
                    if (!dryRun) {
                        existing.get().update(
                                item.arsNumber(), item.name(), item.latitude(),
                                item.longitude(), item.stopType());
                    }
                    result = result.add(1, 0, 1, 0, 0, 0);
                } else {
                    if (!dryRun) {
                        stops.save(new BusStop(
                                item.externalStopId(), item.arsNumber(), item.name(),
                                item.latitude(), item.longitude(), item.stopType()));
                    }
                    result = result.add(1, 1, 0, 0, 0, 0);
                }
            } catch (RuntimeException exception) {
                // 한 원천 행의 오류는 통계에 반영하고 다음 정류장 처리를 계속한다.
                result = result.add(1, 0, 0, 0, 1, 0);
            }
        }
        return result;
    }

    /** 노선과 정차 순서를 하나의 트랜잭션에서 저장한다. */
    @Transactional
    public BusImportReport importRoute(
            NormalizedBusRoute item,
            List<NormalizedBusRouteStop> sequence,
            boolean dryRun) {
        if (item == null) {
            return BusImportReport.empty(ROUTE_PROVIDER, dryRun)
                    .add(1, 0, 0, 1, 0, 0);
        }

        try {
            var existing = routes.findByExternalRouteId(item.externalRouteId());
            boolean insert = existing.isEmpty();
            if (dryRun) {
                return BusImportReport.empty(ROUTE_PROVIDER, true)
                        .add(1, insert ? 1 : 0, insert ? 0 : 1, 0, 0, sequence.size());
            }

            BusRoute route = existing.orElseGet(() -> new BusRoute(
                    item.externalRouteId(), item.routeNumber(), item.routeType(),
                    item.startStopName(), item.endStopName()));
            if (existing.isPresent()) {
                route.update(
                        item.routeNumber(), item.routeType(),
                        item.startStopName(), item.endStopName());
            }
            routes.save(route);

            int linkedStopCount = saveRouteStops(route, sequence);
            return BusImportReport.empty(ROUTE_PROVIDER, false)
                    .add(1, insert ? 1 : 0, insert ? 0 : 1, 0, 0, linkedStopCount);
        } catch (RuntimeException exception) {
            return BusImportReport.empty(ROUTE_PROVIDER, dryRun)
                    .add(1, 0, 0, 0, 1, 0);
        }
    }

    private int saveRouteStops(BusRoute route, List<NormalizedBusRouteStop> sequence) {
        int linkedStopCount = 0;
        for (NormalizedBusRouteStop link : sequence) {
            BusStop stop = stops.findByExternalStopId(link.externalStopId())
                    .orElseGet(() -> stops.save(new BusStop(
                            link.externalStopId(), link.arsNumber(), link.stopName(),
                            link.latitude(), link.longitude(), null)));

            // 노선 응답에 검증된 좌표가 있으면 기존 정류장의 좌표를 보강한다.
            if (link.latitude() != null && link.longitude() != null) {
                stop.enrichCoordinates(
                        link.latitude(), link.longitude(), link.arsNumber(), null);
            }

            var current = routeStops.findByBusRouteIdAndStopSequence(
                    route.getId(), link.sequence());
            if (current.isPresent()) {
                current.get().update(stop, link.direction());
            } else {
                routeStops.save(new BusRouteStop(
                        route, stop, link.sequence(), link.direction()));
            }
            linkedStopCount++;
        }
        return linkedStopCount;
    }

    /** 공식 식별자로 확인된 정류장에만 좌표를 보강한다. */
    @Transactional
    public boolean enrichStopCoordinates(
            String targetExternalId,
            NormalizedBusStop evidence,
            boolean dryRun) {
        if (targetExternalId == null
                || evidence == null
                || evidence.latitude() == null
                || evidence.longitude() == null) {
            return false;
        }

        BusStop target = stops.findByExternalStopId(targetExternalId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "보강 대상 정류장을 찾을 수 없습니다."));
        if (dryRun) {
            return true;
        }

        target.enrichCoordinates(
                evidence.latitude(), evidence.longitude(),
                evidence.arsNumber(), evidence.stopType());
        return true;
    }

    private boolean isValidStop(NormalizedBusStop item) {
        return item != null && item.externalStopId() != null && item.name() != null;
    }
}
