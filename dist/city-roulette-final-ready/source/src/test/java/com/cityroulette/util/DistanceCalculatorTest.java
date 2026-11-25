package com.cityroulette.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DistanceCalculatorTest {

    private static final double GAMCHEON_LATITUDE = 35.0974606978;
    private static final double GAMCHEON_LONGITUDE = 129.0105969965;
    private static final double GWANGALLI_LATITUDE = 35.1531696;
    private static final double GWANGALLI_LONGITUDE = 129.118666;

    @Test
    void returnsZeroForSameCoordinates() {
        double distance = DistanceCalculator.calculateKm(
                GAMCHEON_LATITUDE,
                GAMCHEON_LONGITUDE,
                GAMCHEON_LATITUDE,
                GAMCHEON_LONGITUDE);

        assertThat(distance).isCloseTo(0.0, within(0.000001));
    }

    @Test
    void returnsReasonablePositiveDistanceBetweenBusanCoordinates() {
        double distance = DistanceCalculator.calculateKm(
                GAMCHEON_LATITUDE,
                GAMCHEON_LONGITUDE,
                GWANGALLI_LATITUDE,
                GWANGALLI_LONGITUDE);

        assertThat(distance).isBetween(5.0, 20.0);
    }

    @Test
    void returnsSameDistanceWhenCoordinatesAreReversed() {
        double forward = DistanceCalculator.calculateKm(
                GAMCHEON_LATITUDE,
                GAMCHEON_LONGITUDE,
                GWANGALLI_LATITUDE,
                GWANGALLI_LONGITUDE);
        double backward = DistanceCalculator.calculateKm(
                GWANGALLI_LATITUDE,
                GWANGALLI_LONGITUDE,
                GAMCHEON_LATITUDE,
                GAMCHEON_LONGITUDE);

        assertThat(forward).isCloseTo(backward, within(0.000000001));
    }

    private org.assertj.core.data.Offset<Double> within(double value) {
        return org.assertj.core.data.Offset.offset(value);
    }
}
