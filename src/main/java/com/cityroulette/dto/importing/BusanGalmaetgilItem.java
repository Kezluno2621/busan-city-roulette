package com.cityroulette.dto.importing;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

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
