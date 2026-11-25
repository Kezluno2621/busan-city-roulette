package com.cityroulette.service;

import com.cityroulette.dto.bus.BusRouteStopRaw;
import com.cityroulette.dto.bus.NormalizedBusRouteStop;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

/** 노선별 정류장 응답에서 공식 식별자, 정차 순번, 방향과 좌표를 추출한다. */
@Component
public class BusRouteStopMapper {

    private static final BigDecimal MIN_LATITUDE = new BigDecimal("34.80");
    private static final BigDecimal MAX_LATITUDE = new BigDecimal("35.40");
    private static final BigDecimal MIN_LONGITUDE = new BigDecimal("128.70");
    private static final BigDecimal MAX_LONGITUDE = new BigDecimal("129.35");

    public NormalizedBusRouteStop map(BusRouteStopRaw raw) {
        String id = trim(raw.nodeid());
        String name = trim(raw.bstopnm());
        int sequence = parseSequence(raw.bstopidx());
        if (id == null || name == null || sequence < 1) {
            throw new IllegalArgumentException("노선 정류소 ID, 이름, 순번은 필수입니다.");
        }

        BigDecimal latitude = number(raw.lat());
        BigDecimal longitude = number(raw.lin());
        if (!hasValidBusanCoordinates(latitude, longitude)) {
            latitude = null;
            longitude = null;
        }

        return new NormalizedBusRouteStop(
                id,
                trim(raw.arsno()),
                name,
                sequence,
                trim(raw.direction()),
                latitude,
                longitude);
    }

    private int parseSequence(String value) {
        try {
            return value == null ? -1 : Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            return -1;
        }
    }

    private BigDecimal number(String value) {
        try {
            return value == null || value.isBlank()
                    ? null
                    : new BigDecimal(value.trim()).setScale(7, RoundingMode.HALF_UP);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private boolean hasValidBusanCoordinates(BigDecimal latitude, BigDecimal longitude) {
        return latitude != null
                && longitude != null
                && latitude.compareTo(MIN_LATITUDE) >= 0
                && latitude.compareTo(MAX_LATITUDE) <= 0
                && longitude.compareTo(MIN_LONGITUDE) >= 0
                && longitude.compareTo(MAX_LONGITUDE) <= 0;
    }

    private String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
