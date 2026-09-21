package com.threadspace.backend.integration.vercel;

import java.util.LinkedHashMap;
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

    @Override
    @Transactional
    public Integration update(UUID projectId, Map<String, String> credentials) {
        Integration integration = integrationRepository
                .findByProjectIdAndIntegrationType(projectId, IntegrationType.VERCEL)
                .stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("No Vercel integration found for project"));

        IntegrationSecret secret = integrationSecretRepository
                .findByIntegrationId(integration.getId())
                .orElseThrow(() -> new IllegalStateException("No secret found for integration"));

        VercelSecretPayload current;
        try {
            current = objectMapper.readValue(secret.getSecretJson(), VercelSecretPayload.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize current Vercel secrets", e);
        }

        String apiToken = credentials.get("apiToken");
        String projectName = credentials.get("projectName");
        String teamId = credentials.get("teamId");

        VercelSecretPayload updated = new VercelSecretPayload(
                apiToken != null && !apiToken.isBlank() ? apiToken.trim() : current.apiToken(),
                projectName != null && !projectName.isBlank() ? projectName.trim() : current.projectName(),
                teamId != null && !teamId.isBlank() ? teamId.trim() : current.teamId());

        String json;
        try {
            json = objectMapper.writeValueAsString(updated);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize Vercel secrets", e);
        }

        secret.setSecretJson(json);
        integrationSecretRepository.save(secret);

        return integration;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, String> getDisplayCredentials(UUID projectId) {
        Integration integration = integrationRepository
                .findByProjectIdAndIntegrationType(projectId, IntegrationType.VERCEL)
                .stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("No Vercel integration found for project"));

        IntegrationSecret secret = integrationSecretRepository
                .findByIntegrationId(integration.getId())
                .orElseThrow(() -> new IllegalStateException("No secret found for integration"));

        VercelSecretPayload payload;
        try {
            payload = objectMapper.readValue(secret.getSecretJson(), VercelSecretPayload.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize Vercel secrets", e);
        }

        Map<String, String> result = new LinkedHashMap<>();
        result.put("apiToken", mask(payload.apiToken()));
        result.put("projectName", payload.projectName());
        if (payload.teamId() != null) {
            result.put("teamId", payload.teamId());
        }
        return result;
    }

    private String mask(String value) {
        if (value == null || value.length() < 4) return "****";
        return "****" + value.substring(value.length() - 4);
    }
}
