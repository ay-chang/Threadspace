package com.threadspace.backend.integration.core;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;

@Service
public class IntegrationService {

    private final IntegrationRepository integrationRepository;

    public IntegrationService(IntegrationRepository integrationRepository) {
        this.integrationRepository = integrationRepository;
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

}
