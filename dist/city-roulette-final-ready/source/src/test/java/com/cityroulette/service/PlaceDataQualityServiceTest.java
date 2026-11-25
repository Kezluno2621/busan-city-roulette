package com.cityroulette.service;

import com.cityroulette.domain.*;
import com.cityroulette.repository.*;
import java.time.LocalDateTime;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PlaceDataQualityServiceTest {
    @Autowired PlaceDataQualityService quality;
    @Autowired PlaceRepository places;
    @Autowired PlaceSourceRepository sources;

    @Test void calculatesMissingRatesDistributionsAndProviderCoverage() {
        String suffix = Long.toString(System.nanoTime());
        Place complete = places.save(new Place("BUSAN_ATTRACTION", "quality-a-" + suffix, "완전 장소", "관광지",
                "부산 해운대구", "해운대구", 35.1, 129.1, "설명", false));
        complete.updateFromApi(complete.getName(), complete.getCategory(), complete.getAddress(), complete.getDistrict(),
                complete.getLatitude(), complete.getLongitude(), complete.getDescription(), "https://example.com/a.jpg");
        Place missing = places.save(new Place("BUSAN_RESTAURANT", "quality-b-" + suffix, "누락 장소", "음식점",
                null, null, null, null, null, false));
        PlaceSource source = new PlaceSource(complete, "QUALITY_TEST", "quality-a-" + suffix);
        source.synchronize("https://data.go.kr", "ATTRACTION", null, LocalDateTime.now(), "부산광역시",
                "이용허락범위 제한 없음", "이용허락범위 제한 없음");
        sources.save(source);

        var report = quality.measure();

        assertThat(report.totalPlaces()).isGreaterThanOrEqualTo(2);
        assertThat(report.missingCoordinates()).isGreaterThanOrEqualTo(1);
        assertThat(report.missingAddresses()).isGreaterThanOrEqualTo(1);
        assertThat(report.missingImages()).isGreaterThanOrEqualTo(1);
        assertThat(report.categoryDistribution()).containsKeys("관광지", "음식점");
        assertThat(report.districtDistribution()).containsKey("(미확인)");
        assertThat(report.providers()).anySatisfy(provider -> {
            assertThat(provider.provider()).isEqualTo("QUALITY_TEST");
            assertThat(provider.unknownImageLicenseCount()).isZero();
        });
    }
}
