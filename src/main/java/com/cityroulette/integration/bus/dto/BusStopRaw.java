package com.cityroulette.integration.bus.dto;
/**
 * 외부 응답에서 읽은 원천 값을 담아 이후 검증과 변환 단계로 전달합니다.
 */
public record BusStopRaw(String bstopid,String arsno,String bstopnm,String gpsx,String gpsy,String stoptype) { }
