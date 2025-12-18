package com.threadspace.backend.integration.core;

import java.util.Map;
import java.util.UUID;

public interface IntegrationProvider {

    IntegrationType getType();

    Integration connect(UUID projectId,
            String displayName,
            Map<String, String> credentials);
}
