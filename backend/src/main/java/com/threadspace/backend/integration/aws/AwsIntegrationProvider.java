package com.threadspace.backend.integration.aws;

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
public class AwsIntegrationProvider implements IntegrationProvider {

    private final IntegrationRepository integrationRepository;
    private final IntegrationSecretRepository integrationSecretRepository;
    private final ObjectMapper objectMapper;

    public AwsIntegrationProvider(IntegrationRepository integrationRepository,
            IntegrationSecretRepository integrationSecretRepository,
            ObjectMapper objectMapper) {
        this.integrationRepository = integrationRepository;
        this.integrationSecretRepository = integrationSecretRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public IntegrationType getType() {
        return IntegrationType.AWS;
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

        String accessKeyId = credentials.get("accessKeyId");
        String secretAccessKey = credentials.get("secretAccessKey");
        String region = credentials.get("region");
        String roleArn = credentials.get("roleArn");

        if (accessKeyId == null || accessKeyId.isBlank()) {
            throw new IllegalArgumentException("AWS accessKeyId is required");
        }
        if (secretAccessKey == null || secretAccessKey.isBlank()) {
            throw new IllegalArgumentException("AWS secretAccessKey is required");
        }
        if (region == null || region.isBlank()) {
            throw new IllegalArgumentException("AWS region is required");
        }

        // Create row
        Integration integration = new Integration();
        integration.setDisplayName(displayName.trim());
        integration.setIntegrationType(IntegrationType.AWS);
        integration.setProjectId(projectId);
        integration.setIntegrationStatus(IntegrationStatus.PENDING);

        integration = integrationRepository.save(integration);

        // JSON
        AwsSecretPayload payload = new AwsSecretPayload(
                accessKeyId.trim(),
                secretAccessKey.trim(),
                region.trim(),
                roleArn != null ? roleArn.trim() : null);

        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize AWS secrets", e);
        }

        IntegrationSecret secret = integrationSecretRepository
                .findByIntegrationId(integration.getId())
                .orElseGet(IntegrationSecret::new);

        secret.setIntegrationId(integration.getId());
        secret.setProvider(IntegrationType.AWS);
        secret.setSecretJson(json);

        integrationSecretRepository.save(secret);

        // TODO: Validate AWS credentials 
        integration.setIntegrationStatus(IntegrationStatus.CONNECTED);
        return integrationRepository.save(integration);
    }
}
