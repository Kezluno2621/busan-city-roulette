package com.cityroulette.config;

import com.cityroulette.service.TourismApiClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class TourismApiConfig {

    @Bean
    RestClient tourismRestClient(
            RestClient.Builder builder,
            @Value("${tour.api.base-url}") String baseUrl) {
        return builder.baseUrl(baseUrl).build();
    }

    @Bean
    TourismApiClient tourismApiClient(
            @Qualifier("tourismRestClient") RestClient restClient,
            @Value("${tour.api.service-key:}") String serviceKey,
            @Value("${tour.api.mobile-os:ETC}") String mobileOs,
            @Value("${tour.api.mobile-app:CityRoulette}") String mobileApp) {
        return new TourismApiClient(restClient, serviceKey, mobileOs, mobileApp);
    }
}
