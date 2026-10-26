package com.cityroulette.service;

import com.cityroulette.domain.Place;
import com.cityroulette.dto.place.NearbyPlaceDto;
import com.cityroulette.repository.PlaceRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlaceCandidateServiceTest {

    private static final double START_LATITUDE = 35.0;
    private static final double START_LONGITUDE = 129.0;

    private final PlaceRepository placeRepository = mock(PlaceRepository.class);
    private final PlaceCandidateService service = new PlaceCandidateService(placeRepository);

    @Test
    void sortsPlacesByDistanceAscending() {
        Place far = place("먼 장소", 35.03, 129.0);
        Place near = place("가까운 장소", 35.001, 129.0);
        Place middle = place("중간 장소", 35.01, 129.0);
        when(placeRepository.findByLatitudeIsNotNullAndLongitudeIsNotNull())
                .thenReturn(List.of(far, near, middle));

        List<NearbyPlaceDto> result = service.findNearestPlaces(
                START_LATITUDE, START_LONGITUDE, 3);

        assertThat(result).extracting(NearbyPlaceDto::name)
                .containsExactly("가까운 장소", "중간 장소", "먼 장소");
        assertThat(result).extracting(NearbyPlaceDto::distanceKm)
                .isSorted();
        verify(placeRepository).findByLatitudeIsNotNullAndLongitudeIsNotNull();
    }

    @Test
    void returnsAtMostRequestedLimit() {
        List<Place> places = java.util.stream.IntStream.rangeClosed(1, 7)
                .mapToObj(index -> place(
                        "장소 " + index,
                        START_LATITUDE + index * 0.001,
                        START_LONGITUDE))
                .toList();
        when(placeRepository.findByLatitudeIsNotNullAndLongitudeIsNotNull())
                .thenReturn(places);

        List<NearbyPlaceDto> result = service.findNearestPlaces(
                START_LATITUDE, START_LONGITUDE, 5);

        assertThat(result).hasSize(5);
    }

    @Test
    void coordinateQueryExcludesPlacesWithMissingCoordinates() {
        Place valid = place("좌표 있는 장소", 35.1, 129.1);
        Place missingLatitude = place("위도 없는 장소", null, 129.1);
        Place missingLongitude = place("경도 없는 장소", 35.1, null);
        when(placeRepository.findByLatitudeIsNotNullAndLongitudeIsNotNull())
                .thenReturn(List.of(missingLatitude, valid, missingLongitude));

        List<NearbyPlaceDto> result = service.findNearestPlaces(
                START_LATITUDE, START_LONGITUDE, 5);

        assertThat(result).extracting(NearbyPlaceDto::name)
                .containsExactly("좌표 있는 장소");
    }

    @Test
    void returnsEmptyListWhenDatabaseHasNoCoordinatePlaces() {
        when(placeRepository.findByLatitudeIsNotNullAndLongitudeIsNotNull())
                .thenReturn(List.of());

        assertThat(service.findNearestPlaces(START_LATITUDE, START_LONGITUDE, 5))
                .isEmpty();
    }

    private Place place(String name, Double latitude, Double longitude) {
        return new Place(
                "external-" + name,
                name,
                "관광지",
                "부산광역시",
                latitude,
                longitude,
                null);
    }
}
