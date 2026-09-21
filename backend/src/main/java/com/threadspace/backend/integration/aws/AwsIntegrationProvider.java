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
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.GetCallerIdentityResponse;
import software.amazon.awssdk.services.sts.model.StsException;

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

        // Validate AWS credentials
        verifyCredentials(accessKeyId.trim(), secretAccessKey.trim(), region.trim());

        integration.setIntegrationStatus(IntegrationStatus.CONNECTED);
        return integrationRepository.save(integration);
    }

    @Override
    public Integration update(UUID projectId, Map<String, String> credentials) {
        throw new UnsupportedOperationException("AWS update not yet implemented");
    }

    @Override
    public Map<String, String> getDisplayCredentials(UUID projectId) {
        throw new UnsupportedOperationException("AWS getDisplayCredentials not yet implemented");
    }

    private void verifyCredentials(String accessKeyId, String secretAccessKey, String region) {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKeyId, secretAccessKey);

        try (StsClient stsClient = StsClient.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build()) {

            GetCallerIdentityResponse response = stsClient.getCallerIdentity();
            // Credentials are valid - identity confirmed
            // Account: response.account(), ARN: response.arn()

        } catch (StsException e) {
            throw new IllegalArgumentException("Invalid AWS credentials: " + e.awsErrorDetails().errorMessage(), e);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to verify AWS credentials: " + e.getMessage(), e);
        }
    }
}
