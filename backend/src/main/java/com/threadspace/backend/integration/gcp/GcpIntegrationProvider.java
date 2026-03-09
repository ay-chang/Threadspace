package com.threadspace.backend.integration.gcp;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
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
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("displayName must not be empty");
        }
        if (credentials == null || credentials.isEmpty()) {
            throw new IllegalArgumentException("credentials must not be empty");
        }

        String gcpProjectId = credentials.get("projectId");
        String clientEmail = credentials.get("clientEmail");
        String privateKey = credentials.get("privateKey");

        if (gcpProjectId == null || gcpProjectId.isBlank()) {
            throw new IllegalArgumentException("GCP projectId is required");
        }
        if (clientEmail == null || clientEmail.isBlank()) {
            throw new IllegalArgumentException("GCP clientEmail is required");
        }
        if (privateKey == null || privateKey.isBlank()) {
            throw new IllegalArgumentException("GCP privateKey is required");
        }

        // Create row
        Integration integration = new Integration();
        integration.setDisplayName(displayName.trim());
        integration.setIntegrationType(IntegrationType.GOOGLE_CLOUD);
        integration.setProjectId(projectId);
        integration.setIntegrationStatus(IntegrationStatus.PENDING);

        integration = integrationRepository.save(integration);

        // JSON
        GcpSecretPayload payload = new GcpSecretPayload(
                gcpProjectId.trim(),
                clientEmail.trim(),
                privateKey.trim());

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

        // Validate GCP credentials
        verifyCredentials(gcpProjectId.trim(), clientEmail.trim(), privateKey.trim());

        integration.setIntegrationStatus(IntegrationStatus.CONNECTED);
        return integrationRepository.save(integration);
    }

    private void verifyCredentials(String gcpProjectId, String clientEmail, String privateKey) {
        try {
            String serviceAccountJson = buildServiceAccountJson(gcpProjectId, clientEmail, privateKey);
            GoogleCredentials credentials = GoogleCredentials.fromStream(
                    new ByteArrayInputStream(serviceAccountJson.getBytes(StandardCharsets.UTF_8)));

            Storage storage = StorageOptions.newBuilder()
                    .setProjectId(gcpProjectId)
                    .setCredentials(credentials)
                    .build()
                    .getService();

            // Attempt to list buckets to verify credentials
            storage.list(Storage.BucketListOption.pageSize(1));
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid GCP credentials: " + e.getMessage(), e);
        }
    }

    static String buildServiceAccountJson(String projectId, String clientEmail, String privateKey) {
        return "{"
                + "\"type\": \"service_account\","
                + "\"project_id\": \"" + projectId + "\","
                + "\"client_email\": \"" + clientEmail + "\","
                + "\"private_key\": \"" + privateKey.replace("\\n", "\\n") + "\","
                + "\"token_uri\": \"https://oauth2.googleapis.com/token\""
                + "}";
    }
}
