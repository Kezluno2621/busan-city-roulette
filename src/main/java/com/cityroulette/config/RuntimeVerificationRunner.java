package com.cityroulette.config;

import com.cityroulette.dto.bus.*;
import com.cityroulette.repository.*;
import com.cityroulette.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.*;

@Component
@Profile("dev")
@ConditionalOnProperty(name="runtime.verification.enabled",havingValue="true")
public class RuntimeVerificationRunner implements ApplicationRunner {
    private static final Logger log=LoggerFactory.getLogger(RuntimeVerificationRunner.class);
    private final DataSource dataSource;
    private final JdbcTemplate jdbc;
    private final DataVerificationService verification;
    private final BusCollectionService collection;
    private final BusanBusClient client;
    private final BusRouteMapper routeMapper;
    private final BusRouteStopMapper stopMapper;
    private final BusQueryService busQueries;
    private final PlaceRepository places;
    private final BusStopRepository stops;
    private final BusRouteRepository routes;
    private final BusRouteStopRepository routeStops;
    private final ImportRunRepository importRuns;
    private final String mode;
    private final String expectedDatabase;
    private final boolean backupConfirmed;
    private final String routeNumber;

    public RuntimeVerificationRunner(DataSource dataSource,JdbcTemplate jdbc,DataVerificationService verification,
            BusCollectionService collection,BusanBusClient client,BusRouteMapper routeMapper,BusRouteStopMapper stopMapper,
            BusQueryService busQueries,PlaceRepository places,BusStopRepository stops,BusRouteRepository routes,
            BusRouteStopRepository routeStops,ImportRunRepository importRuns,
            @Value("${runtime.verification.mode:INSPECT}")String mode,
            @Value("${runtime.verification.expected-database:}")String expectedDatabase,
            @Value("${runtime.verification.backup-confirmed:false}")boolean backupConfirmed,
            @Value("${runtime.verification.route-number:}")String routeNumber){
        this.dataSource=dataSource;this.jdbc=jdbc;this.verification=verification;this.collection=collection;
        this.client=client;this.routeMapper=routeMapper;this.stopMapper=stopMapper;this.busQueries=busQueries;
        this.places=places;this.stops=stops;this.routes=routes;this.routeStops=routeStops;this.importRuns=importRuns;
        this.mode=mode.trim().toUpperCase(Locale.ROOT);this.expectedDatabase=expectedDatabase.trim();
        this.backupConfirmed=backupConfirmed;this.routeNumber=routeNumber.trim();
    }

    @Override
    public void run(ApplicationArguments args)throws Exception{
        String database=currentDatabase();
        boolean expectedMatches=!expectedDatabase.isBlank()&&expectedDatabase.equals(database);
        log.info("RUNTIME_VERIFICATION database={} developmentProfile=true expectedDatabaseMatches={} flywayVersion={}",database,expectedMatches,flywayVersion());
        logSnapshot("BEFORE");
        if("INSPECT".equals(mode))return;
        requireRouteNumber();
        BusImportReport stopDry=collection.collectStops(true,10,10);
        BusImportReport routeDry=collection.collectRoutes(true,List.of(routeNumber),1,20);
        logReport("DRY_RUN_STOPS",stopDry);logReport("DRY_RUN_ROUTE",routeDry);
        if(stopDry.fetched()!=10||stopDry.failed()!=0)throw new IllegalStateException("Dry-run stopped: exactly 10 valid stops were not fetched.");
        if(routeDry.fetched()!=1||routeDry.failed()!=0)throw new IllegalStateException("Dry-run stopped: one exact route match was not fetched.");
        if("DRY_RUN".equals(mode))return;
        if(!"WRITE_VERIFY".equals(mode))throw new IllegalArgumentException("runtime.verification.mode must be INSPECT, DRY_RUN, or WRITE_VERIFY");
        if(!expectedMatches)throw new IllegalStateException("Write verification blocked: expected database does not match the connected database.");
        if(!backupConfirmed)throw new IllegalStateException("Write verification blocked: runtime.verification.backup-confirmed is false.");

        List<Long> placeIdsBefore=places.findAllIds();long placeCountBefore=places.count();
        Counts before=counts();
        BusImportReport firstStops=collection.collectStops(false,10,10);
        BusImportReport firstRoute=collection.collectRoutes(false,List.of(routeNumber),1,20);
        Counts afterFirst=counts();
        BusImportReport secondStops=collection.collectStops(false,10,10);
        BusImportReport secondRoute=collection.collectRoutes(false,List.of(routeNumber),1,20);
        Counts afterSecond=counts();
        boolean placesPreserved=placeCountBefore==places.count()&&placeIdsBefore.equals(places.findAllIds());
        boolean duplicateCountsStable=afterFirst.busStops==afterSecond.busStops&&afterFirst.busRoutes==afterSecond.busRoutes&&afterFirst.routeStops==afterSecond.routeStops;
        boolean orderMatches=verifyRouteOrder();
        logReport("FIRST_STOPS",firstStops);logReport("FIRST_ROUTE",firstRoute);
        logReport("SECOND_STOPS",secondStops);logReport("SECOND_ROUTE",secondRoute);
        log.info("RUNTIME_VERIFICATION counts before={} afterFirst={} afterSecond={}",before,afterFirst,afterSecond);
        log.info("RUNTIME_VERIFICATION assertions placesPreserved={} duplicateCountsStable={} routeOrderMatches={}",placesPreserved,duplicateCountsStable,orderMatches);
        if(!placesPreserved||!duplicateCountsStable||!orderMatches)throw new IllegalStateException("Runtime verification assertions failed; inspect the preceding sanitized statistics.");
        logSnapshot("AFTER");
    }

    private boolean verifyRouteOrder(){
        NormalizedBusRoute apiRoute=client.getRoutes(routeNumber).stream().filter(raw->raw.buslinenum()!=null&&raw.buslinenum().trim().equalsIgnoreCase(routeNumber)).map(routeMapper::map).findFirst().orElseThrow(()->new IllegalStateException("No exact route returned for the configured route number."));
        var stored=routes.findByExternalRouteId(apiRoute.externalRouteId()).orElseThrow(()->new IllegalStateException("Imported route was not found."));
        List<String> apiOrder=client.getRouteStops(apiRoute.externalRouteId()).stream().limit(20).map(stopMapper::map).map(x->x.sequence()+":"+x.externalStopId()).toList();
        List<String> dbOrder=busQueries.routeStops(stored.getId()).stream().limit(20).map(x->x.stopSequence()+":"+x.stop().externalStopId()).toList();
        return !apiOrder.isEmpty()&&apiOrder.equals(dbOrder);
    }
    private String currentDatabase()throws Exception{try(Connection connection=dataSource.getConnection()){return connection.getCatalog();}}
    private String flywayVersion(){try{return jdbc.queryForObject("SELECT COALESCE(MAX(version),'NONE') FROM flyway_schema_history WHERE success=1",String.class);}catch(RuntimeException e){return "UNAVAILABLE";}}
    private void requireRouteNumber(){if(routeNumber.isBlank())throw new IllegalStateException("runtime.verification.route-number is required for DRY_RUN and WRITE_VERIFY.");}
    private Counts counts(){return new Counts(stops.count(),routes.count(),routeStops.count(),importRuns.count());}
    private void logReport(String label,BusImportReport report){log.info("RUNTIME_VERIFICATION {} provider={} fetched={} inserted={} updated={} skipped={} failed={} routeStops={}",label,report.provider(),report.fetched(),report.inserted(),report.updated(),report.skipped(),report.failed(),report.routeStops());}
    private void logSnapshot(String label){var value=verification.snapshot();log.info("RUNTIME_VERIFICATION {} places={} placeSources={} locatedPlaces={} busStops={} locatedBusStops={} busRoutes={} routeStops={} routesWithoutStops={} match300={} match500={} match1000={}",label,value.places(),value.placeSources(),value.placesWithCoordinates(),value.busStops(),value.busStopsWithValidCoordinates(),value.busRoutes(),value.busRouteStops(),value.routesWithoutStops(),value.matchedWithin300Meters(),value.matchedWithin500Meters(),value.matchedWithin1000Meters());}
    private record Counts(long busStops,long busRoutes,long routeStops,long importRuns){ }
}
