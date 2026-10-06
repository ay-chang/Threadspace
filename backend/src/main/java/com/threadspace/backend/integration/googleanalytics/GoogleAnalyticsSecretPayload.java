package com.threadspace.backend.integration.googleanalytics;

/**
 * Stored Google Analytics credentials: the GA4 property to read and the service
 * account key used to read it. {@code projectId} and {@code clientEmail} are
 * copied out of the key for display and error messages.
 */
public record GoogleAnalyticsSecretPayload(
        String propertyId,
        String projectId,
        String clientEmail,
        String serviceAccountJson) {
}
