package com.threadspace.backend.integration.stripe;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
 * Minimal read-only client for the Stripe REST API.
 *
 * The base URL is configurable ({@code stripe.api-base}) so tests can point it at
 * a local server. Requests pin {@code Stripe-Version} so response shapes don't
 * shift when the account's default API version is upgraded.
 */
@Component
public class StripeApiClient {

    static final String STRIPE_VERSION = "2024-06-20";

    /** Stripe's maximum page size for list endpoints. */
    private static final int PAGE_SIZE = 100;

    private final String baseUrl;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public StripeApiClient(
            @Value("${stripe.api-base:https://api.stripe.com}") String baseUrl,
            ObjectMapper objectMapper) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.objectMapper = objectMapper;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(30_000);
        this.restTemplate = new RestTemplate(factory);
    }

    /** A page-walked list result. {@code truncated} means more items existed past the cap. */
    public record ListResult(List<JsonNode> items, boolean truncated) {
    }

    public JsonNode get(String apiKey, String path, Map<String, String> params) {
        URI uri = URI.create(baseUrl + path + toQuery(params));

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.set("Stripe-Version", STRIPE_VERSION);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        try {
            String body = restTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<>(headers), String.class)
                    .getBody();
            return objectMapper.readTree(body == null ? "{}" : body);
        } catch (HttpStatusCodeException e) {
            throw toStripeException(e);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException("Stripe returned a response that was not valid JSON", e);
        }
    }

    /**
     * Walks a Stripe list endpoint with cursor pagination until it runs out of
     * items or reaches {@code maxItems}.
     */
    public ListResult list(String apiKey, String path, Map<String, String> params, int maxItems) {
        List<JsonNode> items = new ArrayList<>();
        String startingAfter = null;

        while (true) {
            Map<String, String> pageParams = new LinkedHashMap<>(params);
            pageParams.put("limit", String.valueOf(Math.min(PAGE_SIZE, maxItems - items.size())));
            if (startingAfter != null) {
                pageParams.put("starting_after", startingAfter);
            }

            JsonNode page = get(apiKey, path, pageParams);
            JsonNode data = page.path("data");
            for (JsonNode item : data) {
                items.add(item);
            }

            boolean hasMore = page.path("has_more").asBoolean(false);
            if (!hasMore || data.isEmpty()) {
                return new ListResult(items, false);
            }
            if (items.size() >= maxItems) {
                return new ListResult(items, true);
            }
            startingAfter = data.get(data.size() - 1).path("id").asText();
        }
    }

    private StripeApiException toStripeException(HttpStatusCodeException e) {
        String type = null;
        String code = null;
        String message = null;
        try {
            JsonNode error = objectMapper.readTree(e.getResponseBodyAsString()).path("error");
            type = textOrNull(error, "type");
            code = textOrNull(error, "code");
            message = textOrNull(error, "message");
        } catch (Exception ignored) {
            // Not Stripe's JSON error shape (e.g. a proxy error page); keep the status.
        }
        return new StripeApiException(e.getStatusCode().value(), type, code, message, e);
    }

    private static String textOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    /**
     * Encodes each key and value separately. Stripe uses bracketed keys such as
     * {@code created[gte]}, which Spring's strict URI builder rejects.
     */
    static String toQuery(Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return "";
        }
        return "?" + params.entrySet().stream()
                .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8)
                        + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
    }
}
