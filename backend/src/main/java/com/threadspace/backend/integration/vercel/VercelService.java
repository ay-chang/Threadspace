package com.threadspace.backend.integration.vercel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.threadspace.backend.integration.core.Integration;
import com.threadspace.backend.integration.core.IntegrationRepository;
import com.threadspace.backend.integration.core.IntegrationSecret;
import com.threadspace.backend.integration.core.IntegrationSecretRepository;
import com.threadspace.backend.integration.core.IntegrationType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.threadspace.backend.integration.vercel.VercelDtos.*;

@Service
public class VercelService {

        private final IntegrationRepository integrationRepository;
        private final IntegrationSecretRepository integrationSecretRepository;
        private final VercelApiClient vercelApiClient;
        private final ObjectMapper objectMapper;

        public VercelService(
                        IntegrationRepository integrationRepository,
                        IntegrationSecretRepository integrationSecretRepository,
                        VercelApiClient vercelApiClient,
                        ObjectMapper objectMapper) {
                this.integrationRepository = integrationRepository;
                this.integrationSecretRepository = integrationSecretRepository;
                this.vercelApiClient = vercelApiClient;
                this.objectMapper = objectMapper;
        }

        @Transactional(readOnly = true)
        public ProjectSummary getProjectSummary(UUID projectId) {
                Integration integration = integrationRepository
                                .findByProjectIdAndIntegrationType(projectId, IntegrationType.VERCEL)
                                .stream()
                                .findFirst()
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "No Vercel integration found for project"));

                IntegrationSecret secret = integrationSecretRepository.findByIntegrationId(integration.getId())
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "No Vercel secret found for integration"));

                VercelSecretPayload payload;
                try {
                        payload = objectMapper.readValue(secret.getSecretJson(), VercelSecretPayload.class);
                } catch (Exception e) {
                        throw new RuntimeException("Failed to parse Vercel secrets", e);
                }

                Optional<String> teamId = Optional.ofNullable(payload.teamId()).filter(id -> !id.isBlank());

                VercelProject project = vercelApiClient.get("/v9/projects/" + payload.projectName(), payload.apiToken(),
                                teamId, VercelProject.class);
                VercelDomainsResponse domainsResponse = vercelApiClient.get(
                                "/v9/projects/" + project.id() + "/domains",
                                payload.apiToken(),
                                teamId,
                                VercelDomainsResponse.class);

                VercelEnvResponse envResponse = vercelApiClient.get(
                                "/v10/projects/" + project.id() + "/env",
                                payload.apiToken(),
                                teamId,
                                VercelEnvResponse.class);

                return new ProjectSummary(
                                project.id(),
                                project.name(),
                                project.framework(),
                                project.git() != null ? project.git().repository() : null,
                                domainsResponse != null && domainsResponse.domains() != null ? domainsResponse.domains()
                                                : Collections.emptyList(),
                                envResponse != null && envResponse.envs() != null ? envResponse.envs()
                                                : Collections.emptyList());
        }
}
