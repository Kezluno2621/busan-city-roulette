package com.cityroulette.service;

import com.cityroulette.dto.external.ExternalApiProbeResult;
import com.cityroulette.dto.importing.ProviderPage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.ArrayList;
import java.util.List;

abstract class BusanJsonApiClient {
    private final RestClient restClient;
    private final String provider;
    private final String path;
    private final String key;
    private final ObjectMapper objectMapper = new ObjectMapper();

    BusanJsonApiClient(RestClient restClient, String provider, String path, String key) {
        this.restClient = restClient; this.provider = provider; this.path = path; this.key = key;
    }

    public ExternalApiProbeResult probe() {
        if (key == null || key.isBlank()) return ExternalApiProbeResult.notConfigured(provider);
        try {
            String body = restClient.get().uri(builder -> builder.path(path)
                    .queryParam("ServiceKey", "{key}").queryParam("pageNo", 1)
                    .queryParam("numOfRows", 1).queryParam("resultType", "json")
                    .build(decode(key))).retrieve().body(String.class);
            JsonNode root;
            try { root = objectMapper.readTree(body); }
            catch (JsonProcessingException exception) {
                return ExternalApiProbeResult.failure(provider,
                        ExternalApiProbeResult.Status.PARSE_ERROR, 200, null, false,
                        "JSON 응답을 해석할 수 없습니다.");
            }
            JsonNode envelope = envelope(root);
            String code = validateHeader(envelope.path("header"));
            return ExternalApiProbeResult.connected(provider, itemCount(envelope),
                    responseFields(envelope), code);
        } catch (ExternalApiException exception) {
            ExternalApiProbeResult.Status status = isAuthCode(exception.getProviderCode())
                    ? ExternalApiProbeResult.Status.AUTH_FAILED : ExternalApiProbeResult.Status.UNAVAILABLE;
            return ExternalApiProbeResult.failure(provider, status, 200,
                    exception.getProviderCode(), true, exception.getMessage());
        } catch (RestClientResponseException exception) {
            int status = exception.getStatusCode().value();
            ExternalApiProbeResult.Status resultStatus = status == 401 || status == 403
                    ? ExternalApiProbeResult.Status.AUTH_FAILED : ExternalApiProbeResult.Status.HTTP_ERROR;
            return ExternalApiProbeResult.failure(provider, resultStatus, status, null,
                    false, "외부 API가 HTTP " + status + "을 반환했습니다.");
        } catch (RestClientException | IllegalArgumentException exception) {
            return ExternalApiProbeResult.failure(provider, ExternalApiProbeResult.Status.HTTP_ERROR,
                    null, null, false, "외부 API HTTP 요청에 실패했습니다.");
        }
    }

    protected <T> ProviderPage<T> getPage(int pageNo, int numOfRows, Class<T> itemType) {
        if (pageNo < 1 || numOfRows < 1 || numOfRows > 100) {
            throw new IllegalArgumentException("pageNo는 1 이상, numOfRows는 1~100이어야 합니다.");
        }
        if (key == null || key.isBlank()) {
            throw new ExternalApiException(provider + " API 키가 설정되지 않았습니다.");
        }
        RuntimeException lastFailure = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                String body = restClient.get().uri(builder -> builder.path(path)
                        .queryParam("ServiceKey", "{key}").queryParam("pageNo", pageNo)
                        .queryParam("numOfRows", numOfRows).queryParam("resultType", "json")
                        .build(decode(key))).retrieve().body(String.class);
                JsonNode envelope = envelope(objectMapper.readTree(body));
                validateHeader(envelope.path("header"));
                JsonNode items = itemNode(envelope);
                List<T> converted = new ArrayList<>();
                if (items.isArray()) {
                    for (JsonNode item : items) converted.add(objectMapper.treeToValue(item, itemType));
                } else if (items.isObject()) {
                    converted.add(objectMapper.treeToValue(items, itemType));
                }
                return new ProviderPage<>(converted, pageNo, numOfRows, itemCount(envelope));
            } catch (RestClientResponseException exception) {
                int status = exception.getStatusCode().value();
                if ((status == 429 || status >= 500) && attempt < 3) {
                    lastFailure = new ExternalApiException(provider + " API 일시 오류(HTTP " + status + ")");
                    continue;
                }
                throw new ExternalApiException(provider + " API HTTP 오류: " + status);
            } catch (JsonProcessingException exception) {
                throw new ExternalApiException(provider + " API JSON 파싱에 실패했습니다.");
            } catch (RestClientException exception) {
                if (attempt < 3) { lastFailure = new ExternalApiException(provider + " API 요청에 실패했습니다."); continue; }
                throw new ExternalApiException(provider + " API 요청에 실패했습니다.");
            }
        }
        throw lastFailure == null ? new ExternalApiException(provider + " API 요청에 실패했습니다.") : lastFailure;
    }

    private JsonNode envelope(JsonNode root) {
        if (root == null || root.isNull()) throw new ExternalApiException("응답 본문이 없습니다.");
        if (root.has("response")) return root.path("response");
        Iterator<JsonNode> values = root.elements();
        if (!values.hasNext()) throw new ExternalApiException("응답 래퍼를 확인할 수 없습니다.");
        return values.next();
    }

    private String validateHeader(JsonNode header) {
        String code = text(header, "resultCode", text(header, "code", null));
        if (code == null) throw new ExternalApiException("API 결과 코드가 없습니다.");
        if (!("00".equals(code) || "0000".equals(code))) {
            throw new ExternalApiException("API 결과 코드 오류: code=" + code, code);
        }
        return code;
    }

    private Set<String> responseFields(JsonNode envelope) {
        JsonNode item = itemNode(envelope);
        if (item.isArray()) item = item.path(0);
        Set<String> fields = new LinkedHashSet<>();
        if (item.isObject()) item.fieldNames().forEachRemaining(fields::add);
        return fields;
    }

    private boolean isAuthCode(String code) {
        return "20".equals(code) || "30".equals(code) || "31".equals(code)
                || "SERVICE_ACCESS_DENIED_ERROR".equals(code)
                || "SERVICE_KEY_IS_NOT_REGISTERED_ERROR".equals(code);
    }

    private int itemCount(JsonNode envelope) {
        if (envelope.has("totalCount")) return envelope.path("totalCount").asInt(0);
        JsonNode body = envelope.path("body");
        if (body.has("totalCount")) return body.path("totalCount").asInt(0);
        JsonNode items = itemNode(envelope);
        return items.isArray() ? items.size() : items.isMissingNode() || items.isNull() ? 0 : 1;
    }

    private JsonNode itemNode(JsonNode envelope) {
        if (envelope.has("item")) return envelope.path("item");
        if (envelope.path("items").has("item")) return envelope.path("items").path("item");
        JsonNode body = envelope.path("body");
        if (body.has("item")) return body.path("item");
        return body.path("items").path("item");
    }

    private String text(JsonNode node, String field, String fallback) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? fallback : value.asText();
    }

    private String decode(String value) {
        try { return URLDecoder.decode(value.trim().replace("+", "%2B"), StandardCharsets.UTF_8); }
        catch (IllegalArgumentException exception) { throw new ExternalApiException("API 키 형식이 올바르지 않습니다."); }
    }
}
