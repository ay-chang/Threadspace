package com.threadspace.backend.integration.gcp;

/**
 * Stored GCP credentials. {@code serviceAccountJson} is the service account key
 * file exactly as Google issued it, and is what gets handed to
 * {@code GoogleCredentials.fromStream}. {@code projectId} and {@code clientEmail}
 * are denormalized copies of fields inside that JSON, kept so callers can read
 * them without re-parsing.
 */
public record GcpSecretPayload(
        String projectId,
        String clientEmail,
        String serviceAccountJson) {
}
