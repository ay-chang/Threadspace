package com.threadspace.backend.integration.appstore;

import java.security.interfaces.ECPrivateKey;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

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
public class AppStoreIntegrationProvider implements IntegrationProvider {

    private static final Pattern ISSUER_ID = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
    private static final Pattern KEY_ID = Pattern.compile("^[A-Z0-9]{10}$");
    private static final Pattern VENDOR_NUMBER = Pattern.compile("^[0-9]{6,12}$");

    private final IntegrationRepository integrationRepository;
    private final IntegrationSecretRepository integrationSecretRepository;
    private final AppStoreApiClient appStoreApiClient;
    private final ObjectMapper objectMapper;

    public AppStoreIntegrationProvider(IntegrationRepository integrationRepository,
            IntegrationSecretRepository integrationSecretRepository,
            AppStoreApiClient appStoreApiClient,
            ObjectMapper objectMapper) {
        this.integrationRepository = integrationRepository;
        this.integrationSecretRepository = integrationSecretRepository;
        this.appStoreApiClient = appStoreApiClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public IntegrationType getType() {
        return IntegrationType.APPLE_CONNECT;
    }

    @Override
    @Transactional
    public Integration connect(UUID projectId, String displayName, Map<String, String> credentials) {
        if (projectId == null) {
            throw new IllegalArgumentException("projectId must not be null");
        }
        Map<String, String> c = credentials == null ? Map.of() : credentials;
        AppStoreSecretPayload payload = validate(
                c.get("issuerId"), c.get("keyId"), c.get("privateKey"), c.get("vendorNumber"));
        verify(payload);

        Integration integration = new Integration();
        integration.setDisplayName("App Store Connect");
        integration.setIntegrationType(IntegrationType.APPLE_CONNECT);
        integration.setProjectId(projectId);
        integration.setIntegrationStatus(IntegrationStatus.CONNECTED);
        integration = integrationRepository.save(integration);

        saveSecret(integration.getId(), payload);
        return integration;
    }

    @Override
    @Transactional
    public Integration update(UUID projectId, Map<String, String> credentials) {
        Integration integration = findIntegration(projectId);
        AppStoreSecretPayload current = readSecret(integration.getId());
        Map<String, String> c = credentials == null ? Map.of() : credentials;

        AppStoreSecretPayload merged = validate(
                orCurrent(c.get("issuerId"), current.issuerId()),
                orCurrent(c.get("keyId"), current.keyId()),
                orCurrent(c.get("privateKey"), current.privateKey()),
                orCurrent(c.get("vendorNumber"), current.vendorNumber()));
        verify(merged);

        saveSecret(integration.getId(), merged);
        return integration;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, String> getDisplayCredentials(UUID projectId) {
        AppStoreSecretPayload payload = readSecret(findIntegration(projectId).getId());
        Map<String, String> result = new LinkedHashMap<>();
        result.put("issuerId", payload.issuerId());
        result.put("keyId", payload.keyId());
        result.put("vendorNumber", payload.vendorNumber());
        result.put("privateKey", "****");
        return result;
    }

    /**
     * Checks each field's shape before anything is sent to Apple. Issuer ID and
     * Key ID sit next to each other in App Store Connect and are easy to swap;
     * their formats differ enough to tell which one went where.
     */
    static AppStoreSecretPayload validate(String issuerId, String keyId, String privateKey, String vendorNumber) {
        String issuer = trim(issuerId);
        String key = trim(keyId);
        String vendor = trim(vendorNumber);

        if (issuer.isEmpty()) {
            throw new IllegalArgumentException("Issuer ID is required.");
        }
        if (!ISSUER_ID.matcher(issuer).matches()) {
            throw new IllegalArgumentException(KEY_ID.matcher(issuer).matches()
                    ? "That looks like a Key ID in the Issuer ID field. The Issuer ID is the longer value "
                            + "with dashes, shown above the keys table."
                    : "Issuer ID should look like 57246542-96fe-1a63-e053-0824d011072a.");
        }
        if (key.isEmpty()) {
            throw new IllegalArgumentException("Key ID is required.");
        }
        if (!KEY_ID.matcher(key).matches()) {
            throw new IllegalArgumentException(ISSUER_ID.matcher(key).matches()
                    ? "That looks like an Issuer ID in the Key ID field. The Key ID is the 10-character "
                            + "value in the keys table, next to the key's name."
                    : "Key ID should be 10 uppercase letters and digits, like 2X9R4HXF34.");
        }
        if (vendor.isEmpty()) {
            throw new IllegalArgumentException("Vendor number is required.");
        }
        if (!VENDOR_NUMBER.matcher(vendor).matches()) {
            throw new IllegalArgumentException(
                    "Vendor number should be digits only, like 85012345. Find it under Payments and Financial Reports.");
        }
        // Throws a user-facing message if the key text is unusable.
        AppStoreJwt.parsePrivateKey(privateKey);

        return new AppStoreSecretPayload(issuer, key, privateKey.trim(), vendor);
    }

    /**
     * Probes the two things the metrics page needs: the key is accepted at all,
     * and it's allowed to download sales reports for this vendor number.
     */
    private void verify(AppStoreSecretPayload payload) {
        ECPrivateKey key = AppStoreJwt.parsePrivateKey(payload.privateKey());
        String token = AppStoreJwt.create(payload.issuerId(), payload.keyId(), key, Instant.now());

        try {
            appStoreApiClient.getJson(token, "/v1/apps", Map.of("limit", "1"));
        } catch (AppStoreApiException e) {
            throw new IllegalArgumentException(describeAuthFailure(e), e);
        } catch (ResourceAccessException e) {
            throw unreachable(e);
        }

        // A few days back, so the date is always one Apple has finished processing.
        LocalDate probeDate = LocalDate.now(AppStoreService.REPORT_ZONE).minusDays(3);
        try {
            appStoreApiClient.getDailySalesReport(token, payload.vendorNumber(), probeDate);
        } catch (AppStoreApiException e) {
            throw new IllegalArgumentException(describeSalesFailure(e, payload.vendorNumber()), e);
        } catch (ResourceAccessException e) {
            throw unreachable(e);
        }
    }

    static String describeAuthFailure(AppStoreApiException e) {
        return switch (e.getStatusCode()) {
            case 401 -> "Apple rejected these credentials. Check that the Key ID belongs to this .p8 file "
                    + "and the Issuer ID is from the same team. A revoked key also fails this way.";
            case 403 -> "This API key isn't allowed to read your apps. Create a key with the Sales, Finance, "
                    + "or Admin role.";
            case 429 -> "Apple is rate limiting requests right now. Wait a moment and try again.";
            default -> "App Store Connect returned an error while verifying the key"
                    + (e.getDetail() != null ? ": " + e.getDetail() : " (HTTP " + e.getStatusCode() + ").");
        };
    }

    static String describeSalesFailure(AppStoreApiException e, String vendorNumber) {
        return switch (e.getStatusCode()) {
            case 401, 403 -> "This API key can't download sales reports. Only keys with the Sales, Finance, "
                    + "or Admin role can - create a new key with one of those roles.";
            case 400 -> "Apple didn't accept vendor number " + vendorNumber
                    + (e.getDetail() != null ? ": " + e.getDetail() : ".")
                    + " Check it under Payments and Financial Reports.";
            case 429 -> "Apple is rate limiting requests right now. Wait a moment and try again.";
            default -> "App Store Connect returned an error while checking sales access"
                    + (e.getDetail() != null ? ": " + e.getDetail() : " (HTTP " + e.getStatusCode() + ").");
        };
    }

    private static IllegalArgumentException unreachable(ResourceAccessException e) {
        return new IllegalArgumentException(
                "Couldn't reach App Store Connect to verify the key. Check your connection and try again.", e);
    }

    private Integration findIntegration(UUID projectId) {
        return integrationRepository
                .findByProjectIdAndIntegrationType(projectId, IntegrationType.APPLE_CONNECT)
                .stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("No App Store Connect integration found for project"));
    }

    private void saveSecret(UUID integrationId, AppStoreSecretPayload payload) {
        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize App Store Connect credentials", e);
        }
        IntegrationSecret secret = integrationSecretRepository
                .findByIntegrationId(integrationId)
                .orElseGet(IntegrationSecret::new);
        secret.setIntegrationId(integrationId);
        secret.setProvider(IntegrationType.APPLE_CONNECT);
        secret.setSecretJson(json);
        integrationSecretRepository.save(secret);
    }

    private AppStoreSecretPayload readSecret(UUID integrationId) {
        IntegrationSecret secret = integrationSecretRepository
                .findByIntegrationId(integrationId)
                .orElseThrow(() -> new IllegalStateException("No secret found for App Store Connect integration"));
        try {
            return objectMapper.readValue(secret.getSecretJson(), AppStoreSecretPayload.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to parse stored App Store Connect credentials", e);
        }
    }

    private static String orCurrent(String submitted, String current) {
        return submitted == null || submitted.isBlank() || submitted.startsWith("****") ? current : submitted;
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
