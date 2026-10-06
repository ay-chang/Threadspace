package com.threadspace.backend.integration.core;

import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads a connected integration's stored credentials.
 *
 * {@code secret_json} is mapped with {@code @Lob}, which Postgres stores as a
 * large object - and large objects can only be read inside a transaction
 * ("Large Objects may not be used in auto-commit mode"). This does the read in
 * a short read-only transaction and returns, so callers can then make slow
 * third-party API calls without holding a database connection open.
 */
@Component
public class IntegrationCredentials {

    private final IntegrationRepository integrationRepository;
    private final IntegrationSecretRepository integrationSecretRepository;
    private final ObjectMapper objectMapper;

    public IntegrationCredentials(IntegrationRepository integrationRepository,
            IntegrationSecretRepository integrationSecretRepository,
            ObjectMapper objectMapper) {
        this.integrationRepository = integrationRepository;
        this.integrationSecretRepository = integrationSecretRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * @param name how to refer to the integration in error messages, e.g. "Stripe"
     * @throws IllegalArgumentException if the project has no connected integration of this type
     */
    @Transactional(readOnly = true)
    public <T> T loadConnected(UUID projectId, IntegrationType type, Class<T> payloadType, String name) {
        Integration integration = integrationRepository
                .findByProjectIdAndIntegrationType(projectId, type)
                .stream()
                .filter(i -> i.getIntegrationStatus() == IntegrationStatus.CONNECTED)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No connected " + name + " integration found for project"));

        IntegrationSecret secret = integrationSecretRepository
                .findByIntegrationId(integration.getId())
                .orElseThrow(() -> new IllegalArgumentException(name + " credentials not found"));

        try {
            return objectMapper.readValue(secret.getSecretJson(), payloadType);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse stored " + name + " credentials", e);
        }
    }
}
