package com.threadspace.backend.integration.gcp;

import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.storage.Storage;
import com.threadspace.backend.integration.core.Integration;
import com.threadspace.backend.integration.google.GoogleServiceAccount;
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
    private final GcpStorageClients storageClients;

    public GcpIntegrationProvider(IntegrationRepository integrationRepository,
            IntegrationSecretRepository integrationSecretRepository,
            ObjectMapper objectMapper,
            GcpStorageClients storageClients) {
        this.integrationRepository = integrationRepository;
        this.integrationSecretRepository = integrationSecretRepository;
        this.objectMapper = objectMapper;
        this.storageClients = storageClients;
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

    /** Validates the pasted key file via the shared parser and keeps the fields worth storing. */
    private GcpSecretPayload parseServiceAccount(String serviceAccountJson) {
        GoogleServiceAccount account = GoogleServiceAccount.parse(serviceAccountJson, objectMapper);
        return new GcpSecretPayload(account.projectId(), account.clientEmail(), account.json());
    }

    private void verifyCredentials(GcpSecretPayload payload) {
        try {
            Storage storage = storageClients.create(payload);

            // Attempt to list buckets to verify credentials
            storage.list(Storage.BucketListOption.pageSize(1));
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid GCP credentials: " + e.getMessage(), e);
        }
    }
}
