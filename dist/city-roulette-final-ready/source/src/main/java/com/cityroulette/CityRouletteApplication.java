package com.cityroulette;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class CityRouletteApplication {

    public static void main(String[] args) {
        SpringApplication.run(CityRouletteApplication.class, args);
    }
}
