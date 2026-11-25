package com.cityroulette.service;

import com.cityroulette.config.ExternalApiProperties;
import com.cityroulette.dto.bus.*;
import com.cityroulette.dto.ExternalApiProbeResult;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.*;
import org.w3c.dom.Document;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class BusanBusClient {
    private final RestClient restClient; private final String key; private final BusanBusXmlParser xml;
    @Autowired public BusanBusClient(RestClient.Builder builder,ExternalApiProperties properties,BusanBusXmlParser xml){
        this(builder.baseUrl("https://apis.data.go.kr/6260000/BusanBIMS").build(),properties.getBusan().getBus().getKey(),xml);
    }
    BusanBusClient(RestClient client,String key,BusanBusXmlParser xml){this.restClient=client;this.key=key;this.xml=xml;}

    public BusApiPage<BusStopRaw> getStops(int page,int size,String name,String arsNumber){
        Document d=request("/busStopList",builder->builder.queryParam("pageNo",page).queryParam("numOfRows",size)
                .queryParamIfPresent("bstopnm",optional(name)).queryParamIfPresent("arsno",optional(arsNumber)));
        List<BusStopRaw> items=xml.items(d,e->new BusStopRaw(xml.text(e,"bstopid"),xml.text(e,"arsno"),xml.text(e,"bstopnm"),
                xml.text(e,"gpsx"),xml.text(e,"gpsy"),xml.text(e,"stoptype")));
        return new BusApiPage<>(items,page,size,xml.integer(d,"totalCount"));
    }
    public List<BusRouteRaw> getRoutes(String routeNumber){
        Document d=request("/busInfo",builder->builder.queryParamIfPresent("lineno",optional(routeNumber)));
        return xml.items(d,e->new BusRouteRaw(first(xml.text(e,"lineid"),xml.text(e,"lineId")),xml.text(e,"buslinenum"),
                xml.text(e,"bustype"),xml.text(e,"startpoint"),xml.text(e,"endpoint")));
    }
    public List<BusRouteStopRaw> getRouteStops(String routeId){
        Document d=request("/busInfoByRouteId",builder->builder.queryParam("lineid",routeId));
        return xml.items(d,e->new BusRouteStopRaw(xml.text(e,"nodeid"),xml.text(e,"bstopidx"),xml.text(e,"bstopnm"),
                xml.text(e,"arsno"),first(xml.text(e,"direction"),xml.text(e,"rpoint")),xml.text(e,"lat"),xml.text(e,"lin")));
    }
    public ExternalApiProbeResult probe(){
        if(key==null||key.isBlank())return ExternalApiProbeResult.notConfigured("BUSAN_BUS");
        try{Document d=request("/busStopList",b->b.queryParam("pageNo",1).queryParam("numOfRows",1));
            return ExternalApiProbeResult.connected("BUSAN_BUS",xml.integer(d,"totalCount"),xml.firstItemFields(d),xml.first(d,"resultCode"));
        }catch(ExternalApiException e){ExternalApiProbeResult.Status status=isAuthCode(e.getProviderCode())?ExternalApiProbeResult.Status.AUTH_FAILED:e.getMessage().contains("XML")?ExternalApiProbeResult.Status.PARSE_ERROR:ExternalApiProbeResult.Status.UNAVAILABLE;return ExternalApiProbeResult.failure("BUSAN_BUS",status,200,e.getProviderCode(),false,e.getMessage());}
    }
    private Document request(String path,java.util.function.Function<org.springframework.web.util.UriBuilder,org.springframework.web.util.UriBuilder> query){
        if(key==null||key.isBlank())throw new ExternalApiException("BUSAN_BUS_API_KEY 환경변수가 설정되지 않았습니다.");
        ExternalApiException last=null;
        for(int attempt=1;attempt<=3;attempt++)try{
            String body=restClient.get().uri(builder->query.apply(builder.path(path)).queryParam("serviceKey","{key}").build(decode(key)))
                    .retrieve().body(String.class); Document d=xml.parse(body); validate(d); return d;
        }catch(RestClientResponseException e){int status=e.getStatusCode().value();if((status==429||status>=500)&&attempt<3){last=new ExternalApiException("BUSAN 버스 API 일시 오류: HTTP "+status);continue;}throw new ExternalApiException("BUSAN 버스 API HTTP 오류: "+status);
        }catch(RestClientException e){last=new ExternalApiException("BUSAN 버스 API 요청에 실패했습니다.");if(attempt==3)throw last;}
        throw last==null?new ExternalApiException("BUSAN 버스 API 요청에 실패했습니다."):last;
    }
    private void validate(Document d){String code=xml.first(d,"resultCode");if(!("00".equals(code)||"0000".equals(code)))throw new ExternalApiException("BUSAN 버스 API 결과 코드 오류: code="+(code==null?"UNKNOWN":code),code);}
    private boolean isAuthCode(String code){return "20".equals(code)||"30".equals(code)||"31".equals(code);}
    private java.util.Optional<String> optional(String value){return value==null||value.isBlank()?java.util.Optional.empty():java.util.Optional.of(value.trim());}
    private String first(String a,String b){return a==null?b:a;}
    private String decode(String value){try{return URLDecoder.decode(value.trim().replace("+","%2B"),StandardCharsets.UTF_8);}catch(Exception e){throw new ExternalApiException("API 키 형식이 올바르지 않습니다.");}}
}
