package com.cityroulette.service;

import com.cityroulette.dto.transit.OdsayResponse;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

public class OdsayApiClient {

    private final RestClient restClient;
    private final String apiKey;

    public OdsayApiClient(RestClient restClient, String apiKey) {
        this.restClient = restClient;
        this.apiKey = apiKey;
    }

    public OdsayResponse searchPublicTransit(
            double startLongitude,
            double startLatitude,
            double endLongitude,
            double endLatitude) {
        String key = requireApiKey();

        try {
            OdsayResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/searchPubTransPathT")
                            .queryParam("SX", startLongitude)
                            .queryParam("SY", startLatitude)
                            .queryParam("EX", endLongitude)
                            .queryParam("EY", endLatitude)
                            .queryParam("OPT", 0)
                            .queryParam("SearchType", 0)
                            .queryParam("SearchPathType", 0)
                            .queryParam("lang", 0)
                            .queryParam("apiKey", "{apiKey}")
                            .build(key))
                    .retrieve()
                    .body(OdsayResponse.class);

            validateResponse(response);
            return response;
        } catch (OdsayApiException exception) {
            throw exception;
        } catch (RestClientResponseException exception) {
            throw new OdsayApiException(
                    "ODsay HTTP 오류: status=" + exception.getStatusCode().value(), exception);
        } catch (RestClientException exception) {
            throw new OdsayApiException("ODsay API 호출에 실패했습니다.", exception);
        }
    }

    private String requireApiKey() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new OdsayApiException("ODSAY_API_KEY 환경변수가 설정되지 않았습니다.");
        }
        return apiKey.trim();
    }

    private void validateResponse(OdsayResponse response) {
        if (response == null) {
            throw new OdsayApiException("ODsay API 응답이 없습니다.");
        }
        if (response.error() != null && !response.error().isNull()) {
            JsonNode detail = errorDetail(response.error());
            String code = text(detail, "code");
            throw new OdsayApiException(formatError(detail, code), code);
        }
    }

    private JsonNode errorDetail(JsonNode error) {
        return error.isArray() && !error.isEmpty() ? error.get(0) : error;
    }

    private String formatError(JsonNode detail, String code) {
        String message = text(detail, "message");
        if (message == null) {
            message = text(detail, "msg");
        }

        return "ODsay API 오류: code=" + valueOrUnknown(code)
                + ", message=" + valueOrUnknown(message);
    }

    private String text(JsonNode node, String fieldName) {
        if (node == null || !node.isObject() || !node.hasNonNull(fieldName)) {
            return null;
        }
        return node.get(fieldName).asText();
    }

    private String valueOrUnknown(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }
}
