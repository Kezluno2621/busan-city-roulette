package com.cityroulette.service;

import com.cityroulette.dto.bus.BusStopRaw;
import com.cityroulette.dto.bus.NormalizedBusStop;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

/** 부산 버스 API의 정류장 응답을 DB 저장용 표준 형식으로 변환한다. */
@Component
public class BusStopMapper {

    private static final BigDecimal MIN_LATITUDE = new BigDecimal("34.80");
    private static final BigDecimal MAX_LATITUDE = new BigDecimal("35.40");
    private static final BigDecimal MIN_LONGITUDE = new BigDecimal("128.70");
    private static final BigDecimal MAX_LONGITUDE = new BigDecimal("129.35");

    public NormalizedBusStop map(BusStopRaw raw) {
        String id = trim(raw.bstopid());
        String name = trim(raw.bstopnm());
        if (id == null || name == null) {
            throw new IllegalArgumentException("정류소 ID와 이름은 필수입니다.");
        }

        BigDecimal latitude = number(raw.gpsy());
        BigDecimal longitude = number(raw.gpsx());
        if (!hasValidBusanCoordinates(latitude, longitude)) {
            // 잘못된 좌표를 임의 보정하지 않고 누락 값으로 보존한다.
            latitude = null;
            longitude = null;
        }

        return new NormalizedBusStop(
                id,
                trim(raw.arsno()),
                name,
                latitude,
                longitude,
                trim(raw.stoptype()));
    }

    private boolean hasValidBusanCoordinates(BigDecimal latitude, BigDecimal longitude) {
        return latitude != null
                && longitude != null
                && latitude.compareTo(MIN_LATITUDE) >= 0
                && latitude.compareTo(MAX_LATITUDE) <= 0
                && longitude.compareTo(MIN_LONGITUDE) >= 0
                && longitude.compareTo(MAX_LONGITUDE) <= 0;
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

    private String trim(String value) {
        return value == null || value.isBlank()
                ? null
                : value.trim().replaceAll("\\s+", " ");
    }
}
