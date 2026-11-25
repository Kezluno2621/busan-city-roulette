package com.cityroulette.config;

import com.cityroulette.service.DataVerificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name="data.verification.enabled",havingValue="true")
public class DataVerificationRunner implements ApplicationRunner {
    private static final Logger log=LoggerFactory.getLogger(DataVerificationRunner.class);
    private final DataVerificationService service;

    public DataVerificationRunner(DataVerificationService service){this.service=service;}

    @Override
    public void run(ApplicationArguments args){
        var value=service.snapshot();
        log.info("DATA_VERIFICATION tables places={} placeSources={} busStops={} busRoutes={} busRouteStops={}",
                value.places(),value.placeSources(),value.busStops(),value.busRoutes(),value.busRouteStops());
        log.info("DATA_VERIFICATION coordinates places={} busStops={}",value.placesWithCoordinates(),value.busStopsWithValidCoordinates());
        log.info("DATA_VERIFICATION placeCategories={}",value.placesByCategory());
        log.info("DATA_VERIFICATION placeDistricts={}",value.placesByDistrict());
        log.info("DATA_VERIFICATION routeLinks routesWithStops={} routesWithoutStops={}",value.routesWithStops(),value.routesWithoutStops());
        log.info("DATA_VERIFICATION nearbyMatch percent300m={} percent500m={} percent1000m={}",
                value.matchedWithin300Meters(),value.matchedWithin500Meters(),value.matchedWithin1000Meters());
    }
}
