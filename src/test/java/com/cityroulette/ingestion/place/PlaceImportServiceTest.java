package com.cityroulette.ingestion.place;

import com.cityroulette.ingestion.ImportRunService;
import com.cityroulette.integration.ExternalApiException;
import com.cityroulette.ingestion.place.dto.NormalizedPlace;
import com.cityroulette.domain.place.repository.PlaceMergeCandidateRepository;
import com.cityroulette.domain.place.repository.PlaceRepository;
import com.cityroulette.domain.place.repository.PlaceSourceRepository;
import com.cityroulette.ingestion.ImportRunRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class PlaceImportServiceTest {
    @Autowired PlaceImportService service;
    @Autowired PlaceRepository places;
    @Autowired PlaceSourceRepository sources;
    @Autowired PlaceMergeCandidateRepository candidates;
    @Autowired ImportRunRepository runs;
    @Autowired ImportRunService runService;

    @BeforeEach void clean() { candidates.deleteAll(); sources.deleteAll(); places.deleteAll(); runs.deleteAll(); }

    @Test void insertsThenUpdatesSameSourceWithoutDuplicate() {
        var first=service.importItems("BUSAN_ATTRACTION",List.of(place("1","첫 이름")),false);
        var second=service.importItems("BUSAN_ATTRACTION",List.of(place("1","바뀐 이름")),false);
        assertThat(first.inserted()).isEqualTo(1);
        assertThat(second.updated()).isEqualTo(1);
        assertThat(places.count()).isEqualTo(1);
        assertThat(sources.count()).isEqualTo(1);
        assertThat(places.findAll().get(0).getName()).isEqualTo("바뀐 이름");
    }

    @Test void sameExternalIdFromDifferentProvidersCreatesDistinctSourcesAndPlaces() {
        var attraction = place("BUSAN_ATTRACTION", "258", "명소 258");
        var galmaetgil = place("BUSAN_GALMAETGIL", "258", "갈맷길 258");

        var first = service.importItems("BUSAN_ATTRACTION", List.of(attraction), false);
        var second = service.importItems("BUSAN_GALMAETGIL", List.of(galmaetgil), false);

        assertThat(first.inserted()).isEqualTo(1);
        assertThat(second.inserted()).isEqualTo(1);
        assertThat(places.count()).isEqualTo(2);
        assertThat(sources.count()).isEqualTo(2);
        assertThat(sources.findByProviderAndExternalId("BUSAN_ATTRACTION", "258")).isPresent();
        assertThat(sources.findByProviderAndExternalId("BUSAN_GALMAETGIL", "258")).isPresent();
    }

    @Test void repeatedExternalIdWithinSameProviderUpdatesWithoutAddingRows() {
        service.importItems("BUSAN_GALMAETGIL", List.of(place("BUSAN_GALMAETGIL", "258", "처음 이름")), false);
        var repeated = service.importItems("BUSAN_GALMAETGIL",
                List.of(place("BUSAN_GALMAETGIL", "258", "수정 이름")), false);

        assertThat(repeated.updated()).isEqualTo(1);
        assertThat(places.count()).isEqualTo(1);
        assertThat(sources.count()).isEqualTo(1);
        assertThat(places.findAll()).singleElement()
                .extracting(com.cityroulette.domain.place.entity.Place::getName).isEqualTo("수정 이름");
    }

    @Test void dryRunLeavesEveryImportTableUnchanged() {
        var result=service.importItems("BUSAN_ATTRACTION",List.of(place("2","미리보기")),true);
        assertThat(result.inserted()).isEqualTo(1);
        assertThat(places.count()).isZero();
        assertThat(sources.count()).isZero();
        assertThat(candidates.count()).isZero();
    }

    @Test void skipsNonPlaceContentAndCountsRepeatedSourceInPage() {
        NormalizedPlace theme=new NormalizedPlace(NormalizedPlace.RecordType.THEME_CONTENT,"BUSAN_FOODIE","T1","테마","테마",
                "미식테마",null,null,null,null,null,null,null,"source","theme",null,null,null,"부산광역시");
        var result=service.importItems("BUSAN_FOODIE",List.of(theme),false);
        assertThat(result.skipped()).isEqualTo(1);
        assertThat(places.count()).isZero();
    }

    @Test void recordsFailedRunButDryRunCreatesNoRun() {
        PlaceDataProvider failing=new PlaceDataProvider() {
            public String provider(){ return "FAIL_PROVIDER"; }
            public com.cityroulette.ingestion.place.dto.NormalizedProviderPage fetch(int page,int size){
                throw new ExternalApiException("표본 호출 실패");
            }
        };
        var collection=new PlaceCollectionService(List.of(failing),service,runService);
        assertThatThrownBy(() -> collection.collect("FAIL_PROVIDER",false,10,10))
                .isInstanceOf(ExternalApiException.class);
        assertThat(runs.findAll()).singleElement().satisfies(run -> {
            assertThat(run.getStatus()).isEqualTo("FAILED");
            assertThat(run.getErrorMessage()).isEqualTo("표본 호출 실패");
        });
        runs.deleteAll();
        assertThatThrownBy(() -> collection.collect("FAIL_PROVIDER",true,10,10))
                .isInstanceOf(ExternalApiException.class);
        assertThat(runs.count()).isZero();
    }

    @Test void hidesImageWhenSourceHasNoConfirmedImageLicense() {
        NormalizedPlace item = place("image-unlicensed", "라이선스 미확인");
        item = new NormalizedPlace(item.recordType(), item.provider(), item.externalId(), item.name(),
                item.normalizedName(), item.category(), item.address(), item.normalizedAddress(), item.district(),
                item.latitude(), item.longitude(), item.description(), item.imageUrl(), item.sourceUrl(),
                item.sourceCategory(), item.sourceModifiedAt(), item.contentLicense(), null, item.attribution());

        service.importItems("BUSAN_ATTRACTION", List.of(item), false);

        assertThat(places.findBySourceAndExternalId("BUSAN_ATTRACTION", "image-unlicensed"))
                .get().extracting(com.cityroulette.domain.place.entity.Place::getImageUrl).isNull();
    }

    private NormalizedPlace place(String id,String name) {
        return place("BUSAN_ATTRACTION", id, name);
    }

    private NormalizedPlace place(String provider, String id, String name) {
        return new NormalizedPlace(NormalizedPlace.RecordType.PLACE,provider,id,name,name.replace(" ","").toLowerCase(),
                "관광지","부산 해운대구 해운대로 1","부산해운대구해운대로1","해운대구",35.16,129.16,
                "설명","https://example.com/a.jpg","source","ATTRACTION",null,"공공데이터","공공데이터","부산광역시");
    }
}
