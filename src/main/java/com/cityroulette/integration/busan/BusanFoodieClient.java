package com.cityroulette.integration.busan;
import com.cityroulette.integration.ExternalApiProperties;
import org.springframework.web.client.RestClient;
import org.springframework.stereotype.Component;
import com.cityroulette.integration.busan.dto.BusanFoodieItem;
import com.cityroulette.integration.busan.dto.ProviderPage;
/**
 * 외부 HTTP 요청과 공급자 응답 처리를 애플리케이션 로직에서 분리합니다.
 */
@Component
public class BusanFoodieClient extends BusanJsonApiClient {
    public BusanFoodieClient(RestClient.Builder builder, ExternalApiProperties p) {
        super(builder.baseUrl("https://apis.data.go.kr").build(), "BUSAN_FOODIE",
                "/6260000/FoodieService/getFoodieKr", p.getBusan().getFoodie().getKey());
    }
    public ProviderPage<BusanFoodieItem> getPage(int pageNo, int size) {
        return super.getPage(pageNo, size, BusanFoodieItem.class);
    }
}
