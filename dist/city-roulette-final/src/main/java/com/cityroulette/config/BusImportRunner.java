package com.cityroulette.config;
import com.cityroulette.service.BusCollectionService;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import java.util.*;
@Component @ConditionalOnProperty(name="bus.import.enabled",havingValue="true")
public class BusImportRunner implements ApplicationRunner {
    private static final Logger log=LoggerFactory.getLogger(BusImportRunner.class); private final BusCollectionService service;private final String mode;private final boolean dry;private final int pageSize,limit,routeLimit,routeStopLimit;private final List<String> routeNumbers;
    public BusImportRunner(BusCollectionService service,@Value("${bus.import.mode:STOPS}")String mode,@Value("${bus.import.dry-run:true}")boolean dry,@Value("${bus.import.page-size:10}")int pageSize,@Value("${bus.import.limit:10}")int limit,@Value("${bus.import.route-limit:1}")int routeLimit,@Value("${bus.import.route-stop-limit:20}")int routeStopLimit,@Value("${bus.import.route-numbers:}")String routeNumbers){this.service=service;this.mode=mode.toUpperCase(Locale.ROOT);this.dry=dry;this.pageSize=pageSize;this.limit=limit;this.routeLimit=routeLimit;this.routeStopLimit=routeStopLimit;this.routeNumbers=Arrays.stream(routeNumbers.split(",")).map(String::trim).filter(s->!s.isBlank()).toList();}
    public void run(ApplicationArguments args){if(mode.equals("STOPS")||mode.equals("ALL"))print(service.collectStops(dry,pageSize,limit));if(mode.equals("ROUTES")||mode.equals("ALL"))print(service.collectRoutes(dry,routeNumbers,routeLimit,routeStopLimit));}
    private void print(com.cityroulette.dto.bus.BusImportReport r){log.info("BUS_IMPORT_RESULT provider={} dryRun={} fetched={} inserted={} updated={} skipped={} failed={} routeStops={}",r.provider(),r.dryRun(),r.fetched(),r.inserted(),r.updated(),r.skipped(),r.failed(),r.routeStops());}
}
