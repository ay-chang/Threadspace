package com.threadspace.backend.integration.stripe;

/**
 * Stored Stripe credentials. {@code livemode} is derived from the key prefix at
 * connect time and kept so the UI can label test-mode data without re-parsing.
 */
public record StripeSecretPayload(
        String apiKey,
        boolean livemode) {
}
