package com.cityroulette.integration.busan.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 외부 응답에서 읽은 원천 값을 담아 이후 검증과 변환 단계로 전달합니다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BusanGalmaetgilItem(
        String uc_seq,
        String name,
        String title,
        String place,
        String cate1_nm,
        String lat,
        String lng,
        String course,
        String gugan,
        String main_img_n,
        String itemcntnts) { }
