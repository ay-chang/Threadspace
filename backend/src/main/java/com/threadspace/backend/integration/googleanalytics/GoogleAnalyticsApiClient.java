package com.threadspace.backend.integration.googleanalytics;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

/**
 * Minimal client for the GA4 Data API (v1beta). The base URL is configurable
 * ({@code google-analytics.api-base}) so tests can point it at a local server.
 */
@Component
public class GoogleAnalyticsApiClient {

    public static final String SCOPE = "https://www.googleapis.com/auth/analytics.readonly";

    private final String baseUrl;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public GoogleAnalyticsApiClient(
            @Value("${google-analytics.api-base:https://analyticsdata.googleapis.com}") String baseUrl,
            ObjectMapper objectMapper) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.objectMapper = objectMapper;

        // java.net.http rather than HttpURLConnection: the latter discards the
        // body of a 401 that carries WWW-Authenticate on a POST, which is exactly
        // how the Data API reports a bad token - leaving no error to parse.
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
        factory.setReadTimeout(Duration.ofSeconds(30));
        this.restTemplate = new RestTemplate(factory);
    }

    public JsonNode runReport(String token, String propertyId, JsonNode request) {
        return post(token, "/v1beta/properties/" + propertyId + ":runReport", request);
    }

    /** Up to five reports in one round trip; the response's {@code reports} keep request order. */
    public JsonNode batchRunReports(String token, String propertyId, List<JsonNode> requests) {
        ObjectNode body = objectMapper.createObjectNode();
        body.putArray("requests").addAll(requests);
        return post(token, "/v1beta/properties/" + propertyId + ":batchRunReports", body);
    }

    public JsonNode runRealtimeReport(String token, String propertyId, JsonNode request) {
        return post(token, "/v1beta/properties/" + propertyId + ":runRealtimeReport", request);
    }

    private JsonNode post(String token, String path, JsonNode body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        try {
            String response = restTemplate.exchange(URI.create(baseUrl + path), HttpMethod.POST,
                    new HttpEntity<>(objectMapper.writeValueAsString(body), headers), String.class).getBody();
            return objectMapper.readTree(response == null ? "{}" : response);
        } catch (HttpStatusCodeException e) {
            throw toApiException(e);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException("Google Analytics returned a response that was not valid JSON", e);
        }
    }

    private GoogleAnalyticsApiException toApiException(HttpStatusCodeException e) {
        String status = null;
        String reason = null;
        String message = null;
        try {
            JsonNode error = objectMapper.readTree(e.getResponseBodyAsString()).path("error");
            status = error.hasNonNull("status") ? error.get("status").asText() : null;
            message = error.hasNonNull("message") ? error.get("message").asText() : null;
            for (JsonNode detail : error.path("details")) {
                if (detail.hasNonNull("reason")) {
                    reason = detail.get("reason").asText();
                    break;
                }
            }
        } catch (Exception ignored) {
            // Not Google's JSON error shape; keep the HTTP status.
        }
        return new GoogleAnalyticsApiException(e.getStatusCode().value(), status, reason, message, e);
    }
}
