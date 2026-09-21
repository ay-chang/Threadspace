package com.threadspace.backend.integration.core;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IntegrationService {

    private final IntegrationRepository integrationRepository;
    private final IntegrationSecretRepository integrationSecretRepository;
    private final Map<IntegrationType, IntegrationProvider> providerByType; 

    public IntegrationService(IntegrationRepository integrationRepository,
            IntegrationSecretRepository integrationSecretRepository,
            List<IntegrationProvider> providers) {
        this.integrationRepository = integrationRepository;
        this.integrationSecretRepository = integrationSecretRepository;
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

    /**
     * Disconnects an integration: the stored credentials and the integration row
     * are both removed. The projectId is part of the lookup so one project cannot
     * delete another project's integration by guessing an id.
     */
    @Transactional
    public void deleteIntegration(UUID projectId, UUID integrationId) {
        if (projectId == null) {
            throw new IllegalArgumentException("projectId must not be null");
        }
        if (integrationId == null) {
            throw new IllegalArgumentException("integrationId must not be null");
        }

        Integration integration = integrationRepository.findById(integrationId)
                .filter(i -> projectId.equals(i.getProjectId()))
                .orElseThrow(() -> new IllegalArgumentException(
                        "No integration " + integrationId + " found for project " + projectId));

        // Secret first: it references the integration, and leaving an orphaned
        // credential behind is worse than leaving an orphaned integration row.
        integrationSecretRepository.deleteByIntegrationId(integration.getId());
        integrationRepository.delete(integration);
    }

}
