package com.cityroulette.service;
import com.cityroulette.config.ExternalApiProperties;
import org.springframework.web.client.RestClient;
import org.springframework.stereotype.Component;
import com.cityroulette.dto.importing.BusanAttractionItem;
import com.cityroulette.dto.importing.ProviderPage;
/**
 * 외부 HTTP 요청과 공급자 응답 처리를 애플리케이션 로직에서 분리합니다.
 */
@Component
public class BusanAttractionClient extends BusanJsonApiClient {
    public BusanAttractionClient(RestClient.Builder builder, ExternalApiProperties p) {
        super(builder.baseUrl("https://apis.data.go.kr").build(), "BUSAN_ATTRACTION",
                "/6260000/AttractionService/getAttractionKr", p.getBusan().getAttraction().getKey());
    }
    public ProviderPage<BusanAttractionItem> getPage(int pageNo, int size) {
        return super.getPage(pageNo, size, BusanAttractionItem.class);
    }
}
