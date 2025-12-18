package com.threadspace.backend.integration.vercel;

public record VercelSecretPayload(
                String apiToken,
                String projectName) {
}
