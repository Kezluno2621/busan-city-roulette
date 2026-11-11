package com.cityroulette.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

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
