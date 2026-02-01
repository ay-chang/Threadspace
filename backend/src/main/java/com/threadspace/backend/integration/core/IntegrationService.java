package com.threadspace.backend.integration.core;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

@Service
public class IntegrationService {

    private final IntegrationRepository integrationRepository;
    private final Map<IntegrationType, IntegrationProvider> providerByType; 

    public IntegrationService(IntegrationRepository integrationRepository, List<IntegrationProvider> providers) {
        this.integrationRepository = integrationRepository;
        this.providerByType = providers.stream().collect(Collectors.toMap(IntegrationProvider::getType, p -> p)); // {VERCEL -> VI, AWS -> AWSI}
    }

    public Integration createIntegration(String displayName, IntegrationType integrationType, UUID projectId) {
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("Integrations must have a display name");
        }
        if (integrationType == null) {
            throw new IllegalArgumentException("Integrations must have a type");
        }
        if (projectId == null) {
            throw new IllegalArgumentException("Ingegrations must be part of a project");
        }

        Integration integration = new Integration();
        integration.setDisplayName(displayName);
        integration.setIntegrationType(integrationType);
        integration.setProjectId(projectId);
        integration.setIntegrationStatus(IntegrationStatus.PENDING); // default value for new integration

        return integrationRepository.save(integration);
    }

    public List<Integration> getIntegrationsForProject(UUID projectId) {
        Objects.requireNonNull(projectId, "projectId must not be null");
        return integrationRepository.findByProjectId(projectId);
    }

    public Integration connectIntegration(
            UUID projectId,
            IntegrationType type, // VERCEL
            String displayName,
            Map<String, String> credentials) {
        if (projectId == null) {
            throw new IllegalArgumentException("projectId must not be null");
        }
        if (type == null) {
            throw new IllegalArgumentException("integration type must not be null");
        }

        // {VERCEL -> Vercel Instance (VercelIntegrationProver)}

        IntegrationProvider provider = providerByType.get(type);
        if (provider == null) {
            throw new IllegalStateException("No provider registered for type: " + type);
        }

        return provider.connect(projectId, displayName, credentials);
    }

    public Integration updateIntegration(
            UUID projectId,
            IntegrationType type,
            Map<String, String> credentials) {
        if (projectId == null) {
            throw new IllegalArgumentException("projectId must not be null");
        }
        if (type == null) {
            throw new IllegalArgumentException("integration type must not be null");
        }

        IntegrationProvider provider = providerByType.get(type);
        if (provider == null) {
            throw new IllegalStateException("No provider registered for type: " + type);
        }

        return provider.update(projectId, credentials);
    }

    public Map<String, String> getDisplayCredentials(UUID projectId, IntegrationType type) {
        if (projectId == null) {
            throw new IllegalArgumentException("projectId must not be null");
        }
        if (type == null) {
            throw new IllegalArgumentException("integration type must not be null");
        }

        IntegrationProvider provider = providerByType.get(type);
        if (provider == null) {
            throw new IllegalStateException("No provider registered for type: " + type);
        }

        return provider.getDisplayCredentials(projectId);
    }

}
