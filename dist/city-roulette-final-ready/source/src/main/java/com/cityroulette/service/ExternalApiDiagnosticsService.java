package com.cityroulette.service;

import com.cityroulette.dto.ExternalApiProbeResult;
import com.cityroulette.dto.tourism.TourismApiResponse;
import com.cityroulette.dto.tourism.TourismPlaceDto;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;

@Service
public class ExternalApiDiagnosticsService {
    private final TourismApiClient tourism;
    private final BusanAttractionClient attraction;
    private final BusanRestaurantClient restaurant;
    private final BusanFoodieClient foodie;
    private final BusanGalmaetgilClient galmaetgil;
    private final BusanBusClient bus;

    public ExternalApiDiagnosticsService(TourismApiClient tourism,
            BusanAttractionClient attraction, BusanRestaurantClient restaurant,
            BusanFoodieClient foodie, BusanGalmaetgilClient galmaetgil, BusanBusClient bus) {
        this.tourism = tourism; this.attraction = attraction;
        this.restaurant = restaurant; this.foodie = foodie; this.galmaetgil = galmaetgil; this.bus = bus;
    }

    public List<ExternalApiProbeResult> probeAll() {
        return List.of(probeTour(), attraction.probe(), restaurant.probe(), foodie.probe(),
                galmaetgil.probe(), bus.probe());
    }

    private ExternalApiProbeResult probeTour() {
        if (!tourism.isConfigured()) return ExternalApiProbeResult.notConfigured("TOUR_API");
        try {
            TourismApiResponse response = tourism.getBusanPlaces(1, 1);
            int count = response.response().body() == null ? 0 : response.response().body().totalCount();
            TourismPlaceDto sample = response.response().body() == null
                    || response.response().body().items() == null
                    || response.response().body().items().item() == null
                    || response.response().body().items().item().isEmpty()
                    ? null : response.response().body().items().item().get(0);
            return ExternalApiProbeResult.connected("TOUR_API", count, fieldsOf(sample),
                    response.response().header().resultCode());
        } catch (TourismApiException exception) {
            String code = codeFrom(exception.getMessage());
            ExternalApiProbeResult.Status status = isAuthCode(code)
                    ? ExternalApiProbeResult.Status.AUTH_FAILED : ExternalApiProbeResult.Status.UNAVAILABLE;
            return ExternalApiProbeResult.failure("TOUR_API", status, 200, code, true,
                    code == null ? "TourAPI 응답 검증에 실패했습니다."
                            : "TourAPI 결과 코드 오류: code=" + code);
        } catch (RestClientResponseException exception) {
            int status = exception.getStatusCode().value();
            ExternalApiProbeResult.Status resultStatus = status == 401 || status == 403
                    ? ExternalApiProbeResult.Status.AUTH_FAILED : ExternalApiProbeResult.Status.HTTP_ERROR;
            return ExternalApiProbeResult.failure("TOUR_API", resultStatus, status, null, false,
                    "외부 API가 HTTP " + status + "을 반환했습니다.");
        } catch (RestClientException exception) {
            return ExternalApiProbeResult.failure("TOUR_API", ExternalApiProbeResult.Status.HTTP_ERROR,
                    null, null, false, "외부 API HTTP 요청에 실패했습니다.");
        }
    }

    private Set<String> fieldsOf(TourismPlaceDto item) {
        Set<String> fields = new LinkedHashSet<>();
        if (item == null) return fields;
        add(fields, "contentid", item.contentid()); add(fields, "contenttypeid", item.contenttypeid());
        add(fields, "title", item.title()); add(fields, "addr1", item.addr1()); add(fields, "addr2", item.addr2());
        add(fields, "mapx", item.mapx()); add(fields, "mapy", item.mapy());
        add(fields, "firstimage", item.firstimage()); add(fields, "firstimage2", item.firstimage2());
        add(fields, "cpyrhtDivCd", item.cpyrhtDivCd()); add(fields, "lDongRegnCd", item.lDongRegnCd());
        add(fields, "lDongSignguCd", item.lDongSignguCd()); add(fields, "lclsSystm1", item.lclsSystm1());
        add(fields, "lclsSystm2", item.lclsSystm2()); add(fields, "lclsSystm3", item.lclsSystm3());
        add(fields, "tel", item.tel()); add(fields, "zipcode", item.zipcode());
        add(fields, "createdtime", item.createdtime()); add(fields, "modifiedtime", item.modifiedtime());
        return fields;
    }
    private void add(Set<String> fields, String name, String value) {
        if (value != null) fields.add(name);
    }
    private String codeFrom(String message) {
        if (message == null) return null;
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("resultCode=([^, ]+)").matcher(message);
        return matcher.find() ? matcher.group(1) : null;
    }
    private boolean isAuthCode(String code) {
        return "20".equals(code) || "30".equals(code) || "31".equals(code);
    }
}
