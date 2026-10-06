package com.threadspace.backend.integration.stripe;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.threadspace.backend.integration.core.Integration;
import com.threadspace.backend.integration.core.IntegrationProvider;
import com.threadspace.backend.integration.core.IntegrationRepository;
import com.threadspace.backend.integration.core.IntegrationSecret;
import com.threadspace.backend.integration.core.IntegrationSecretRepository;
import com.threadspace.backend.integration.core.IntegrationStatus;
import com.threadspace.backend.integration.core.IntegrationType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.ResourceAccessException;

@Service
public class StripeIntegrationProvider implements IntegrationProvider {

    /**
     * Every endpoint the metrics page reads, with the name Stripe's restricted
     * key editor uses for the permission that unlocks it. Each is probed at
     * connect time so a missing permission fails there, by name, rather than as
     * an error on the metrics page later.
     */
    private static final List<String[]> REQUIRED_READS = List.of(
            new String[] { "/v1/balance", "Balance" },
            new String[] { "/v1/charges", "Charges" },
            new String[] { "/v1/subscriptions", "Subscriptions" },
            new String[] { "/v1/customers", "Customers" });

    private final IntegrationRepository integrationRepository;
    private final IntegrationSecretRepository integrationSecretRepository;
    private final StripeApiClient stripeApiClient;
    private final ObjectMapper objectMapper;

    public StripeIntegrationProvider(IntegrationRepository integrationRepository,
            IntegrationSecretRepository integrationSecretRepository,
            StripeApiClient stripeApiClient,
            ObjectMapper objectMapper) {
        this.integrationRepository = integrationRepository;
        this.integrationSecretRepository = integrationSecretRepository;
        this.stripeApiClient = stripeApiClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public IntegrationType getType() {
        return IntegrationType.STRIPE;
    }

    @Override
    @Transactional
    public Integration connect(UUID projectId, String displayName, Map<String, String> credentials) {
        if (projectId == null) {
            throw new IllegalArgumentException("projectId must not be null");
        }
        String apiKey = validateKeyFormat(credentials == null ? null : credentials.get("apiKey"));
        verifyKey(apiKey);

        boolean livemode = isLiveKey(apiKey);

        Integration integration = new Integration();
        integration.setDisplayName(livemode ? "Stripe" : "Stripe (test mode)");
        integration.setIntegrationType(IntegrationType.STRIPE);
        integration.setProjectId(projectId);
        integration.setIntegrationStatus(IntegrationStatus.CONNECTED);
        integration = integrationRepository.save(integration);

        saveSecret(integration.getId(), new StripeSecretPayload(apiKey, livemode));
        return integration;
    }

    @Override
    @Transactional
    public Integration update(UUID projectId, Map<String, String> credentials) {
        Integration integration = integrationRepository
                .findByProjectIdAndIntegrationType(projectId, IntegrationType.STRIPE)
                .stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("No Stripe integration found for project"));

        String submitted = credentials == null ? null : credentials.get("apiKey");
        if (submitted == null || submitted.isBlank()) {
            // Nothing to change: the key is the only credential.
            return integration;
        }

        String apiKey = validateKeyFormat(submitted);
        verifyKey(apiKey);

        boolean livemode = isLiveKey(apiKey);
        saveSecret(integration.getId(), new StripeSecretPayload(apiKey, livemode));
        integration.setDisplayName(livemode ? "Stripe" : "Stripe (test mode)");
        return integrationRepository.save(integration);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, String> getDisplayCredentials(UUID projectId) {
        Integration integration = integrationRepository
                .findByProjectIdAndIntegrationType(projectId, IntegrationType.STRIPE)
                .stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("No Stripe integration found for project"));

        StripeSecretPayload payload = readSecret(integration.getId());
        Map<String, String> result = new LinkedHashMap<>();
        result.put("apiKey", mask(payload.apiKey()));
        result.put("mode", payload.livemode() ? "live" : "test");
        return result;
    }

    /**
     * Rejects the keys people commonly paste by mistake, with a message naming
     * what they pasted instead of a generic "invalid key".
     */
    static String validateKeyFormat(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("A Stripe API key is required.");
        }
        String key = raw.trim();

        if (key.startsWith("pk_")) {
            throw new IllegalArgumentException(
                    "That's a publishable key (pk_...), which can't read account data. "
                            + "Use a restricted key (rk_...) or secret key (sk_...) instead.");
        }
        if (key.startsWith("whsec_")) {
            throw new IllegalArgumentException(
                    "That's a webhook signing secret (whsec_...), not an API key. "
                            + "Use a restricted key (rk_...) or secret key (sk_...) instead.");
        }
        boolean knownPrefix = key.startsWith("rk_") || key.startsWith("sk_");
        boolean knownMode = key.contains("_live_") || key.contains("_test_");
        if (!knownPrefix || !knownMode || key.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException(
                    "That doesn't look like a Stripe API key. It should start with "
                            + "rk_live_, rk_test_, sk_live_, or sk_test_.");
        }
        return key;
    }

    static boolean isLiveKey(String apiKey) {
        return apiKey.contains("_live_");
    }

    /** Probes each endpoint the metrics page needs; throws a user-facing message on the first failure. */
    private void verifyKey(String apiKey) {
        for (String[] read : REQUIRED_READS) {
            String path = read[0];
            String permission = read[1];
            try {
                Map<String, String> params = path.equals("/v1/balance") ? Map.of() : Map.of("limit", "1");
                stripeApiClient.get(apiKey, path, params);
            } catch (StripeApiException e) {
                throw new IllegalArgumentException(describeVerificationFailure(e, permission), e);
            } catch (ResourceAccessException e) {
                throw new IllegalArgumentException(
                        "Couldn't reach Stripe to verify the key. Check your connection and try again.", e);
            }
        }
    }

    static String describeVerificationFailure(StripeApiException e, String permission) {
        return switch (e.getStatusCode()) {
            case 401 -> "Stripe rejected this key. It may have been rolled or deleted - "
                    + "create a new key in the Stripe dashboard and try again.";
            case 403 -> "This key doesn't have permission to read " + permission + ". "
                    + "Edit the restricted key in Stripe and set " + permission + " to Read.";
            case 429 -> "Stripe is rate limiting requests right now. Wait a moment and try again.";
            default -> "Stripe returned an error while verifying the key"
                    + (e.getStripeMessage() != null ? ": " + e.getStripeMessage() : " (HTTP " + e.getStatusCode() + ").");
        };
    }

    private void saveSecret(UUID integrationId, StripeSecretPayload payload) {
        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize Stripe credentials", e);
        }

        IntegrationSecret secret = integrationSecretRepository
                .findByIntegrationId(integrationId)
                .orElseGet(IntegrationSecret::new);
        secret.setIntegrationId(integrationId);
        secret.setProvider(IntegrationType.STRIPE);
        secret.setSecretJson(json);
        integrationSecretRepository.save(secret);
    }

    private StripeSecretPayload readSecret(UUID integrationId) {
        IntegrationSecret secret = integrationSecretRepository
                .findByIntegrationId(integrationId)
                .orElseThrow(() -> new IllegalStateException("No secret found for Stripe integration"));
        try {
            return objectMapper.readValue(secret.getSecretJson(), StripeSecretPayload.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to parse stored Stripe credentials", e);
        }
    }

    private static String mask(String value) {
        if (value == null || value.length() < 12) {
            return "****";
        }
        // Keep the prefix (rk_live_ / sk_test_) so the mode stays recognisable.
        int prefixEnd = value.indexOf('_', value.indexOf('_') + 1) + 1;
        return value.substring(0, prefixEnd) + "****" + value.substring(value.length() - 4);
    }
}
