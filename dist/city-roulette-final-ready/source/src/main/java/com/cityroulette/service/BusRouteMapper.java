package com.cityroulette.service;

import com.cityroulette.dto.bus.BusRouteRaw;
import com.cityroulette.dto.bus.NormalizedBusRoute;
import org.springframework.stereotype.Component;

/** 부산 버스 노선 검색 응답을 저장 가능한 표준 노선으로 변환한다. */
@Component
public class BusRouteMapper {

    public NormalizedBusRoute map(BusRouteRaw raw) {
        String id = trim(raw.lineid());
        String number = trim(raw.buslinenum());
        if (id == null || number == null) {
            throw new IllegalArgumentException("노선 ID와 번호는 필수입니다.");
        }

        return new NormalizedBusRoute(
                id,
                number,
                trim(raw.bustype()),
                trim(raw.startpoint()),
                trim(raw.endpoint()));
    }

    private String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
