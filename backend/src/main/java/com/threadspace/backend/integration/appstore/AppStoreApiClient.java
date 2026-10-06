package com.threadspace.backend.integration.appstore;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

/**
 * Minimal read-only client for the App Store Connect API. The base URL is
 * configurable ({@code appstore.api-base}) so tests can point it at a local server.
 */
@Component
public class AppStoreApiClient {

    /** Summary sales reports are versioned; 1_0 is the daily SALES/SUMMARY format. */
    static final String SALES_REPORT_VERSION = "1_0";

    private static final MediaType GZIP = MediaType.parseMediaType("application/a-gzip");

    private final String baseUrl;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public AppStoreApiClient(
            @Value("${appstore.api-base:https://api.appstoreconnect.apple.com}") String baseUrl,
            ObjectMapper objectMapper) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.objectMapper = objectMapper;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(60_000);
        this.restTemplate = new RestTemplate(factory);
    }

    public JsonNode getJson(String token, String path, Map<String, String> params) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        try {
            String body = restTemplate.exchange(uri(path, params), HttpMethod.GET, new HttpEntity<>(headers),
                    String.class).getBody();
            return objectMapper.readTree(body == null ? "{}" : body);
        } catch (HttpStatusCodeException e) {
            throw toAppStoreException(e);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException("App Store Connect returned a response that was not valid JSON", e);
        }
    }

    /**
     * Downloads the daily summary sales report for one date (Pacific time).
     * Returns empty when Apple has no report for that date - which is how it
     * answers both "no sales that day" and "not generated yet".
     */
    public Optional<byte[]> getDailySalesReport(String token, String vendorNumber, LocalDate reportDate) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("filter[frequency]", "DAILY");
        params.put("filter[reportDate]", reportDate.toString());
        params.put("filter[reportSubType]", "SUMMARY");
        params.put("filter[reportType]", "SALES");
        params.put("filter[vendorNumber]", vendorNumber);
        params.put("filter[version]", SALES_REPORT_VERSION);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setAccept(List.of(GZIP, MediaType.APPLICATION_JSON));
        try {
            byte[] body = restTemplate.exchange(uri("/v1/salesReports", params), HttpMethod.GET,
                    new HttpEntity<>(headers), byte[].class).getBody();
            return Optional.ofNullable(body);
        } catch (HttpStatusCodeException e) {
            if (e.getStatusCode().value() == 404) {
                return Optional.empty();
            }
            throw toAppStoreException(e);
        }
    }

    private AppStoreApiException toAppStoreException(HttpStatusCodeException e) {
        String code = null;
        String detail = null;
        try {
            JsonNode first = objectMapper.readTree(e.getResponseBodyAsString()).path("errors").path(0);
            code = first.hasNonNull("code") ? first.get("code").asText() : null;
            detail = first.hasNonNull("detail") ? first.get("detail").asText()
                    : first.hasNonNull("title") ? first.get("title").asText() : null;
        } catch (Exception ignored) {
            // Not Apple's JSON error shape; keep the status code.
        }
        return new AppStoreApiException(e.getStatusCode().value(), code, detail, e);
    }

    /** Encodes keys and values individually; Apple's filter[...] keys contain brackets. */
    private URI uri(String path, Map<String, String> params) {
        String query = params == null || params.isEmpty() ? "" : "?" + params.entrySet().stream()
                .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8)
                        + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
        return URI.create(baseUrl + path + query);
    }
}
