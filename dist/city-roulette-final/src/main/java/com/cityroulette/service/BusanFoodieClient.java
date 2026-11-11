package com.cityroulette.service;
import com.cityroulette.config.ExternalApiProperties;
import org.springframework.web.client.RestClient;
import org.springframework.stereotype.Component;
import com.cityroulette.dto.importing.BusanFoodieItem;
import com.cityroulette.dto.importing.ProviderPage;
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
