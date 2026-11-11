package com.cityroulette.service;
import com.cityroulette.dto.bus.*;
import org.springframework.stereotype.Component;
@Component public class BusRouteMapper {
    public NormalizedBusRoute map(BusRouteRaw raw){String id=trim(raw.lineid()),number=trim(raw.buslinenum());if(id==null||number==null)throw new IllegalArgumentException("노선 ID와 번호는 필수입니다.");return new NormalizedBusRoute(id,number,trim(raw.bustype()),trim(raw.startpoint()),trim(raw.endpoint()));}
    private String trim(String v){return v==null||v.isBlank()?null:v.trim().replaceAll("\\s+"," ");}
}
