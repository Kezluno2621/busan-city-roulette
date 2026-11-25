package com.cityroulette.integration.bus.dto;
/**
 * 외부 응답에서 읽은 원천 값을 담아 이후 검증과 변환 단계로 전달합니다.
 */
public record BusRouteStopRaw(String nodeid,String bstopidx,String bstopnm,String arsno,String direction,String lat,String lin) { }
