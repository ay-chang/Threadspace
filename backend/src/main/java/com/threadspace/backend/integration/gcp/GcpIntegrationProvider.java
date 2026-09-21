package com.threadspace.backend.integration.gcp;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.threadspace.backend.integration.core.Integration;
import com.threadspace.backend.integration.core.IntegrationProvider;
import com.threadspace.backend.integration.core.IntegrationRepository;
import com.threadspace.backend.integration.core.IntegrationSecret;
import com.threadspace.backend.integration.core.IntegrationSecretRepository;
import com.threadspace.backend.integration.core.IntegrationStatus;
import com.threadspace.backend.integration.core.IntegrationType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GcpIntegrationProvider implements IntegrationProvider {

    /**
     * Fields ServiceAccountCredentials.fromJson requires. Missing any of these
     * fails deep inside the Google auth library with a message that does not name
     * the integration, so they are checked up front instead.
     */
    private static final List<String> REQUIRED_KEYS = List.of(
            "client_id",
            "client_email",
            "private_key",
            "private_key_id",
            "project_id");

    private final IntegrationRepository integrationRepository;
    private final IntegrationSecretRepository integrationSecretRepository;
    private final ObjectMapper objectMapper;

    public GcpIntegrationProvider(IntegrationRepository integrationRepository,
            IntegrationSecretRepository integrationSecretRepository,
            ObjectMapper objectMapper) {
        this.integrationRepository = integrationRepository;
        this.integrationSecretRepository = integrationSecretRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public IntegrationType getType() {
        return IntegrationType.GOOGLE_CLOUD;
    }

    @Override
    @Transactional
    public Integration connect(UUID projectId, String displayName, Map<String, String> credentials) {
        if (projectId == null) {
            throw new IllegalArgumentException("projectId must not be null");
        }
        if (credentials == null || credentials.isEmpty()) {
            throw new IllegalArgumentException("credentials must not be empty");
        }

        String serviceAccountJson = credentials.get("serviceAccountJson");
        if (serviceAccountJson == null || serviceAccountJson.isBlank()) {
            throw new IllegalArgumentException("Service account JSON is required");
        }
        serviceAccountJson = serviceAccountJson.trim();

        GcpSecretPayload payload = parseServiceAccount(serviceAccountJson);

        // Validate against GCP before persisting anything as CONNECTED.
        verifyCredentials(payload);

        // The project_id inside the key file is authoritative, so it names the
        // integration regardless of what the client sent.
        Integration integration = new Integration();
        integration.setDisplayName(payload.projectId());
        integration.setIntegrationType(IntegrationType.GOOGLE_CLOUD);
        integration.setProjectId(projectId);
        integration.setIntegrationStatus(IntegrationStatus.CONNECTED);

        integration = integrationRepository.save(integration);

        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize GCP secrets", e);
        }

        IntegrationSecret secret = integrationSecretRepository
                .findByIntegrationId(integration.getId())
                .orElseGet(IntegrationSecret::new);

        secret.setIntegrationId(integration.getId());
        secret.setProvider(IntegrationType.GOOGLE_CLOUD);
        secret.setSecretJson(json);

        integrationSecretRepository.save(secret);

        return integration;
    }

    /**
     * Not implemented, matching AwsIntegrationProvider. The manage/update screen
     * is Vercel-only today and nothing links to it, so there is no exercised path
     * here to support yet.
     */
    @Override
    public Integration update(UUID projectId, Map<String, String> credentials) {
        throw new UnsupportedOperationException("Google Cloud update not yet implemented");
    }

    @Override
    public Map<String, String> getDisplayCredentials(UUID projectId) {
        throw new UnsupportedOperationException("Google Cloud getDisplayCredentials not yet implemented");
    }

    /**
     * Validates the pasted key file and pulls out the fields worth storing
     * alongside it. Rejects anything missing a field the Google auth library
     * needs, so the user gets a message naming the field rather than a parser
     * error.
     */
    private GcpSecretPayload parseServiceAccount(String serviceAccountJson) {
        JsonNode node;
        try {
            node = objectMapper.readTree(serviceAccountJson);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(
                    "Service account JSON is not valid JSON. Paste the downloaded key file exactly as-is.", e);
        }

        if (!node.isObject()) {
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

        return new GcpSecretPayload(
                node.get("project_id").asText(),
                node.get("client_email").asText(),
                serviceAccountJson);
    }

    private void verifyCredentials(GcpSecretPayload payload) {
        try {
            Storage storage = createStorageClient(payload);

            // Attempt to list buckets to verify credentials
            storage.list(Storage.BucketListOption.pageSize(1));
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid GCP credentials: " + e.getMessage(), e);
        }
    }

    /**
     * Builds a GCS client from a stored key file. Shared with GcpStorageService so
     * both paths authenticate identically.
     */
    static Storage createStorageClient(GcpSecretPayload payload) {
        try {
            GoogleCredentials credentials = GoogleCredentials.fromStream(
                    new ByteArrayInputStream(
                            payload.serviceAccountJson().getBytes(StandardCharsets.UTF_8)));

            return StorageOptions.newBuilder()
                    .setProjectId(payload.projectId())
                    .setCredentials(credentials)
                    .build()
                    .getService();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create GCS client: " + e.getMessage(), e);
        }
    }
}
