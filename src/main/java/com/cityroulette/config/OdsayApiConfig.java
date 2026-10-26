package com.cityroulette.config;

import com.cityroulette.service.OdsayApiClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class OdsayApiConfig {

    @Bean
    RestClient odsayRestClient(
            RestClient.Builder builder,
            @Value("${odsay.api.base-url}") String baseUrl) {
        return builder.baseUrl(baseUrl).build();
    }

    @Bean
    OdsayApiClient odsayApiClient(
            @Qualifier("odsayRestClient") RestClient restClient,
            @Value("${odsay.api.key:}") String apiKey) {
        return new OdsayApiClient(restClient, apiKey);
    }
}
