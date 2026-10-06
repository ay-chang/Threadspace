package com.threadspace.backend.integration.appstore;

/**
 * Stored App Store Connect API credentials. {@code privateKey} is the PEM text of
 * the .p8 file Apple issues; the backend signs a fresh JWT with it per request.
 */
public record AppStoreSecretPayload(
        String issuerId,
        String keyId,
        String privateKey,
        String vendorNumber) {
}
