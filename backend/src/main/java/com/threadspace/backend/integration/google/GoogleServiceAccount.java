package com.threadspace.backend.integration.google;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;

/**
 * A pasted Google Cloud service account key file, validated field by field so a
 * bad paste produces a message naming the problem rather than a parser error.
 * Shared by every Google integration (Cloud Storage, Analytics).
 */
public record GoogleServiceAccount(String projectId, String clientEmail, String json) {

    /** Fields ServiceAccountCredentials.fromJson requires, plus project_id. */
    private static final List<String> REQUIRED_KEYS = List.of(
            "client_id",
            "client_email",
            "private_key",
            "private_key_id",
            "project_id");

    public static GoogleServiceAccount parse(String rawJson, ObjectMapper objectMapper) {
        if (rawJson == null || rawJson.isBlank()) {
            throw new IllegalArgumentException("Service account JSON is required");
        }
        String json = rawJson.trim();

        JsonNode node;
        try {
            node = objectMapper.readTree(json);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(
                    "Service account JSON is not valid JSON. Paste the downloaded key file exactly as-is.", e);
        }

        if (node == null || !node.isObject()) {
            throw new IllegalArgumentException("Service account JSON must be a JSON object");
        }

        String type = node.path("type").asText("");
        if (!"service_account".equals(type)) {
            throw new IllegalArgumentException(
                    "Expected a service account key file (\"type\": \"service_account\"), got \"" + type + "\"");
        }

        for (String key : REQUIRED_KEYS) {
            JsonNode value = node.get(key);
            if (value == null || value.asText("").isBlank()) {
                throw new IllegalArgumentException(
                        "Service account JSON is missing required field \"" + key + "\"");
            }
        }

        return new GoogleServiceAccount(
                node.get("project_id").asText(),
                node.get("client_email").asText(),
                json);
    }

    /**
     * Credentials for this account that request tokens from {@code tokenServerUri}.
     *
     * The key file's own {@code token_uri} is deliberately ignored: it's
     * user-supplied, and honouring it would let a crafted file make the backend
     * send requests to an arbitrary address.
     */
    public GoogleCredentials credentials(URI tokenServerUri, List<String> scopes) throws IOException {
        GoogleCredentials parsed = GoogleCredentials.fromStream(
                new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
        if (!(parsed instanceof ServiceAccountCredentials serviceAccount)) {
            throw new IllegalArgumentException("Expected a service account key file");
        }
        return serviceAccount.toBuilder()
                .setTokenServerUri(tokenServerUri)
                .setScopes(scopes)
                .build();
    }
}
