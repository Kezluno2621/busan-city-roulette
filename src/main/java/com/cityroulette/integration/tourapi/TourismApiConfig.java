package com.cityroulette.integration.tourapi;

import com.cityroulette.integration.ExternalApiProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.web.client.RestClient;

/** TourAPI 호출에 사용할 HTTP 클라이언트를 구성합니다. */
@Configuration
@EnableConfigurationProperties(ExternalApiProperties.class)
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
            ExternalApiProperties properties,
            @Value("${tour.api.service-key:}") String legacyServiceKey,
            @Value("${tour.api.mobile-os:ETC}") String mobileOs,
            @Value("${tour.api.mobile-app:CityRoulette}") String mobileApp) {
        String serviceKey = properties.getTour().isConfigured()
                ? properties.getTour().getKey() : legacyServiceKey;
        return new TourismApiClient(restClient, serviceKey, mobileOs, mobileApp);
    }
}
