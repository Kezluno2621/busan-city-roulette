package com.cityroulette.service;
import com.cityroulette.dto.bus.*;
import org.springframework.stereotype.Component;
import java.math.*;
@Component public class BusRouteStopMapper {
    public NormalizedBusRouteStop map(BusRouteStopRaw raw){String id=trim(raw.nodeid()),name=trim(raw.bstopnm());int sequence=parse(raw.bstopidx());if(id==null||name==null||sequence<1)throw new IllegalArgumentException("노선 정류소 ID, 이름, 순번은 필수입니다.");BigDecimal lat=number(raw.lat()),lon=number(raw.lin());if(!valid(lat,lon)){lat=null;lon=null;}return new NormalizedBusRouteStop(id,trim(raw.arsno()),name,sequence,trim(raw.direction()),lat,lon);}
    private int parse(String v){try{return Integer.parseInt(v);}catch(Exception e){return -1;}}
    private BigDecimal number(String v){try{return v==null||v.isBlank()?null:new BigDecimal(v.trim()).setScale(7,RoundingMode.HALF_UP);}catch(NumberFormatException e){return null;}}
    private boolean valid(BigDecimal lat,BigDecimal lon){return lat!=null&&lon!=null&&lat.compareTo(new BigDecimal("34.80"))>=0&&lat.compareTo(new BigDecimal("35.40"))<=0&&lon.compareTo(new BigDecimal("128.70"))>=0&&lon.compareTo(new BigDecimal("129.35"))<=0;}
    private String trim(String v){return v==null||v.isBlank()?null:v.trim().replaceAll("\\s+"," ");}
}
