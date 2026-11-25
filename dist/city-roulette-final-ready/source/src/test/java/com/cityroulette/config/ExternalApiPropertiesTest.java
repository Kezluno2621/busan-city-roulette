package com.cityroulette.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ExternalApiPropertiesTest {
    @Test
    void bindsAllSixProviderKeys() {
        ExternalApiProperties properties = new Binder(new MapConfigurationPropertySource(Map.of(
                "external.api.tour.key", "tour",
                "external.api.busan.attraction.key", "attraction",
                "external.api.busan.restaurant.key", "restaurant",
                "external.api.busan.foodie.key", "foodie",
                "external.api.busan.galmaetgil.key", "galmaetgil",
                "external.api.busan.bus.key", "bus")))
                .bind("external.api", ExternalApiProperties.class)
                .orElseThrow(() -> new IllegalStateException("설정 바인딩 실패"));

        assertThat(properties.getTour().getKey()).isEqualTo("tour");
        assertThat(properties.getBusan().getAttraction().getKey()).isEqualTo("attraction");
        assertThat(properties.getBusan().getRestaurant().getKey()).isEqualTo("restaurant");
        assertThat(properties.getBusan().getFoodie().getKey()).isEqualTo("foodie");
        assertThat(properties.getBusan().getGalmaetgil().getKey()).isEqualTo("galmaetgil");
        assertThat(properties.getBusan().getBus().getKey()).isEqualTo("bus");
    }

    @Test
    void emptyKeysAreNotConfigured() {
        assertThat(new ExternalApiProperties().getTour().isConfigured()).isFalse();
    }
}
