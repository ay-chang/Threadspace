package com.threadspace.backend.integration.core;

import java.time.OffsetDateTime;
import java.util.UUID;

public class IntegrationDtos {

    public record IntegrationCreateRequest(
            String displayName,
            IntegrationType integrationType) {
    }

    public record IntegrationResponse(
            UUID id,
            UUID projectId,
            IntegrationType integrationType,
            IntegrationStatus status,
            String displayName,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt) {
    }

}
