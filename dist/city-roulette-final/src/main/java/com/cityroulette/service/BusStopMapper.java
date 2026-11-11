package com.cityroulette.service;
import com.cityroulette.dto.bus.*;
import org.springframework.stereotype.Component;
import java.math.*;
@Component public class BusStopMapper {
    public NormalizedBusStop map(BusStopRaw raw){
        String id=trim(raw.bstopid()),name=trim(raw.bstopnm()); if(id==null||name==null)throw new IllegalArgumentException("정류소 ID와 이름은 필수입니다.");
        BigDecimal lat=number(raw.gpsy()),lon=number(raw.gpsx());
        if(!valid(lat,lon)){lat=null;lon=null;}
        return new NormalizedBusStop(id,trim(raw.arsno()),name,lat,lon,trim(raw.stoptype()));
    }
    private boolean valid(BigDecimal lat,BigDecimal lon){return lat!=null&&lon!=null&&lat.compareTo(new BigDecimal("34.80"))>=0&&lat.compareTo(new BigDecimal("35.40"))<=0&&lon.compareTo(new BigDecimal("128.70"))>=0&&lon.compareTo(new BigDecimal("129.35"))<=0;}
    private BigDecimal number(String v){try{return v==null||v.isBlank()?null:new BigDecimal(v.trim()).setScale(7,RoundingMode.HALF_UP);}catch(NumberFormatException e){return null;}}
    private String trim(String v){return v==null||v.isBlank()?null:v.trim().replaceAll("\\s+"," ");}
}
