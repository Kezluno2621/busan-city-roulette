package com.cityroulette.config;

import com.cityroulette.service.KakaoLocalApiClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/** 카카오 장소 검색에 사용할 HTTP 클라이언트를 구성합니다. */
@Configuration
public class KakaoLocalApiConfig {

    @Bean
    RestClient kakaoLocalRestClient(
            RestClient.Builder builder,
            @Value("${kakao.local.base-url}") String baseUrl) {
        return builder.baseUrl(baseUrl).build();
    }

    @Bean
    KakaoLocalApiClient kakaoLocalApiClient(
            @Qualifier("kakaoLocalRestClient") RestClient restClient,
            @Value("${kakao.local.rest-api-key:}") String restApiKey) {
        return new KakaoLocalApiClient(restClient, restApiKey);
    }
}
