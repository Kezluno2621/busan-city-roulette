package com.cityroulette.service;
import com.cityroulette.config.ExternalApiProperties;
import org.springframework.web.client.RestClient;
import org.springframework.stereotype.Component;
import com.cityroulette.dto.importing.BusanRestaurantItem;
import com.cityroulette.dto.importing.ProviderPage;
@Component
public class BusanRestaurantClient extends BusanJsonApiClient {
    public BusanRestaurantClient(RestClient.Builder builder, ExternalApiProperties p) {
        super(builder.baseUrl("https://apis.data.go.kr").build(), "BUSAN_RESTAURANT",
                "/6260000/FoodService/getFoodKr", p.getBusan().getRestaurant().getKey());
    }
    public ProviderPage<BusanRestaurantItem> getPage(int pageNo, int size) {
        return super.getPage(pageNo, size, BusanRestaurantItem.class);
    }
}
