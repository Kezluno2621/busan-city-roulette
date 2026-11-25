package com.cityroulette.integration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 외부 API 연결 환경의 설정을 한곳에 바인딩합니다. 호출 클라이언트는 필요한 공급자 설정만 꺼내 사용합니다.
 */
@ConfigurationProperties(prefix = "external.api")
public class ExternalApiProperties {
    private final Credential tour = new Credential();
    private final Busan busan = new Busan();
    public Credential getTour() { return tour; }
    public Busan getBusan() { return busan; }

    public static class Busan {
        private final Credential attraction = new Credential();
        private final Credential restaurant = new Credential();
        private final Credential foodie = new Credential();
        private final Credential galmaetgil = new Credential();
        private final Credential bus = new Credential();
        public Credential getAttraction() { return attraction; }
        public Credential getRestaurant() { return restaurant; }
        public Credential getFoodie() { return foodie; }
        public Credential getGalmaetgil() { return galmaetgil; }
        public Credential getBus() { return bus; }
    }

    public static class Credential {
        private String key = "";
        public String getKey() { return key; }
        public void setKey(String key) { this.key = key == null ? "" : key; }
        public boolean isConfigured() { return !key.isBlank(); }
    }
}
