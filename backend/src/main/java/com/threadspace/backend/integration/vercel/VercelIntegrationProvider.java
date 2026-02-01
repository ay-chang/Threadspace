package com.threadspace.backend.integration.vercel;

import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
public class VercelIntegrationProvider implements IntegrationProvider {

    private final IntegrationRepository integrationRepository;
    private final IntegrationSecretRepository integrationSecretRepository;
    private final ObjectMapper objectMapper;

    public VercelIntegrationProvider(IntegrationRepository integrationRepository,
            IntegrationSecretRepository integrationSecretRepository,
            ObjectMapper objectMapper) {
        this.integrationRepository = integrationRepository;
        this.integrationSecretRepository = integrationSecretRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public IntegrationType getType() {
        return IntegrationType.VERCEL;
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

        String apiToken = credentials.get("apiToken");
        String projectName = credentials.get("projectName");
        String teamId = credentials.get("teamId");

        if (apiToken == null || apiToken.isBlank()) {
            throw new IllegalArgumentException("Vercel apiToken is required");
        }
        if (projectName == null || projectName.isBlank()) {
            throw new IllegalArgumentException("Vercel projectName is required");
        }

        // 1) Create integration row (PENDING)
        Integration integration = new Integration();
        integration.setDisplayName(displayName.trim());
        integration.setIntegrationType(IntegrationType.VERCEL);
        integration.setProjectId(projectId);
        integration.setIntegrationStatus(IntegrationStatus.PENDING);

        integration = integrationRepository.save(integration);

        // 2) Serialize secrets as JSON
        VercelSecretPayload payload = new VercelSecretPayload(
                apiToken.trim(),
                projectName.trim(),
                teamId != null && !teamId.isBlank() ? teamId.trim() : null);

        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize Vercel secrets", e);
        }

        IntegrationSecret secret = integrationSecretRepository
                .findByIntegrationId(integration.getId())
                .orElseGet(IntegrationSecret::new);

        secret.setIntegrationId(integration.getId());
        secret.setProvider(IntegrationType.VERCEL);
        secret.setSecretJson(json);

        integrationSecretRepository.save(secret);

        // TODO later: call Vercel to validate credentials; if invalid, throw here.

        integration.setIntegrationStatus(IntegrationStatus.CONNECTED);
        return integrationRepository.save(integration);
    }
}
