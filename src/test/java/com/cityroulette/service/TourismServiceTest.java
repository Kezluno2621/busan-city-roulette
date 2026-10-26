package com.cityroulette.service;

import com.cityroulette.dto.tourism.TourismApiResponse;
import com.cityroulette.dto.tourism.TourismBody;
import com.cityroulette.dto.tourism.TourismHeader;
import com.cityroulette.dto.tourism.TourismItems;
import com.cityroulette.dto.tourism.TourismPlaceDto;
import com.cityroulette.dto.tourism.TourismResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TourismServiceTest {

    private final TourismApiClient client = mock(TourismApiClient.class);
    private final TourismService service = new TourismService(client);

    @Test
    void extractsTourismPlaceList() {
        TourismPlaceDto place = placeDto();
        TourismApiResponse response = response(new TourismItems(List.of(place)));
        when(client.getBusanTouristPlaces(3, 20)).thenReturn(response);

        List<TourismPlaceDto> result = service.getBusanTouristPlaces(3, 20);

        assertThat(result).containsExactly(place);
        verify(client).getBusanTouristPlaces(3, 20);
    }

    @Test
    void returnsEmptyListWhenItemsAreMissing() {
        when(client.getBusanTouristPlaces(1, 10))
                .thenReturn(response(null));

        assertThat(service.getBusanTouristPlaces(1, 10)).isEmpty();
    }

    @Test
    void returnsEmptyListWhenItemArrayIsMissing() {
        when(client.getBusanTouristPlaces(1, 10))
                .thenReturn(response(new TourismItems(null)));

        assertThat(service.getBusanTouristPlaces(1, 10)).isEmpty();
    }

    @Test
    void returnsEmptyListWhenBodyIsMissing() {
        when(client.getBusanTouristPlaces(1, 10))
                .thenReturn(new TourismApiResponse(new TourismResponse(
                        new TourismHeader("0000", "OK"), null)));

        assertThat(service.getBusanTouristPlaces(1, 10)).isEmpty();
    }

    private TourismApiResponse response(TourismItems items) {
        return new TourismApiResponse(new TourismResponse(
                new TourismHeader("0000", "OK"),
                new TourismBody(items, 10, 1, 0)));
    }

    private TourismPlaceDto placeDto() {
        return new TourismPlaceDto(
                "3017435", "12", "중리노을전망대", "부산광역시 영도구 동삼동", "632-3",
                "129.0643", "35.0692", "", "", "", "26", "200",
                "VE", "VE01", "VE010100", "", "49113", "", "");
    }
}
