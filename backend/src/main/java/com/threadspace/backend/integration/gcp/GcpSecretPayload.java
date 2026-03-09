package com.threadspace.backend.integration.gcp;

public record GcpSecretPayload(
        String projectId,
        String clientEmail,
        String privateKey) {
}
