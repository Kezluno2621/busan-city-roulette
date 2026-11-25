package com.cityroulette.config;

import com.cityroulette.repository.BusRouteRepository;
import com.cityroulette.service.BusStopEnrichmentService;
import com.cityroulette.service.TransitGraphService;
import java.sql.Connection;
import javax.sql.DataSource;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
@ConditionalOnProperty(name="bus.stop-enrichment.enabled",havingValue="true")
public class BusStopEnrichmentRunner implements ApplicationRunner {
    private static final Logger log=LoggerFactory.getLogger(BusStopEnrichmentRunner.class);
    private final DataSource dataSource;
    private final BusRouteRepository routes;
    private final BusStopEnrichmentService enrichment;
    private final TransitGraphService graph;
    private final String routeNumber;
    private final int limit;
    private final boolean dryRun;
    private final String expectedDatabase;
    private final boolean backupConfirmed;

    public BusStopEnrichmentRunner(DataSource dataSource,BusRouteRepository routes,
            BusStopEnrichmentService enrichment,TransitGraphService graph,
            @Value("${bus.stop-enrichment.route-number:}")String routeNumber,
            @Value("${bus.stop-enrichment.limit:20}")int limit,
            @Value("${bus.stop-enrichment.dry-run:true}")boolean dryRun,
            @Value("${runtime.verification.expected-database:}")String expectedDatabase,
            @Value("${runtime.verification.backup-confirmed:false}")boolean backupConfirmed){
        this.dataSource=dataSource;this.routes=routes;this.enrichment=enrichment;this.graph=graph;
        this.routeNumber=routeNumber.trim();this.limit=limit;this.dryRun=dryRun;
        this.expectedDatabase=expectedDatabase.trim();this.backupConfirmed=backupConfirmed;
    }

    @Override public void run(ApplicationArguments args)throws Exception{
        if(routeNumber.isBlank())throw new IllegalStateException("bus.stop-enrichment.route-number is required.");
        String database;
        try(Connection connection=dataSource.getConnection()){database=connection.getCatalog();}
        boolean expectedMatches=!expectedDatabase.isBlank()&&expectedDatabase.equals(database);
        if(!dryRun&&(!expectedMatches||!backupConfirmed))
            throw new IllegalStateException("Bus stop enrichment write blocked: database or backup confirmation failed.");
        var matches=routes.findAllByRouteNumberIgnoreCase(routeNumber);
        if(matches.size()!=1)throw new IllegalStateException("Exactly one stored route must match the configured route number.");
        var report=enrichment.enrichRoute(matches.get(0).getId(),limit,dryRun);
        var view=graph.route(matches.get(0).getId());
        long located=view.stops().stream().filter(x->x.hasCoordinates()).count();
        log.info("BUS_STOP_ENRICHMENT dryRun={} targets={} externalIdMatches={} arsMatches={} enriched={} unresolved={} reviewCandidates={} failed={}",
                report.dryRun(),report.targets(),report.matchedByExternalId(),report.matchedByArsNumber(),report.enriched(),
                report.unresolved(),report.reviewCandidates(),report.failed());
        log.info("BUS_GRAPH routeStops={} locatedStops={} sequenceContinuous={} repeatedStops={}",
                view.stops().size(),located,view.sequenceContinuous(),view.repeatedStops());
    }
}
