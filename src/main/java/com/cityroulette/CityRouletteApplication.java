package com.cityroulette;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/** 웹 서버와 애플리케이션 설정을 시작합니다. */
@SpringBootApplication
@ConfigurationPropertiesScan
public class CityRouletteApplication {

    public static void main(String[] args) {
        SpringApplication.run(CityRouletteApplication.class, args);
    }
}
