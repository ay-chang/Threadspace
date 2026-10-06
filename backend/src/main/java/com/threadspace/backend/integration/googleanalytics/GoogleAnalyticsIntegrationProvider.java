package com.threadspace.backend.integration.googleanalytics;

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
import com.threadspace.backend.integration.google.GoogleAccessTokens;
import com.threadspace.backend.integration.google.GoogleServiceAccount;
import com.threadspace.backend.integration.google.GoogleTokenException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.ResourceAccessException;

@Service
public class GoogleAnalyticsIntegrationProvider implements IntegrationProvider {

    private static final Pattern PROPERTY_ID = Pattern.compile("^[0-9]{5,15}$");

    private final IntegrationRepository integrationRepository;
    private final IntegrationSecretRepository integrationSecretRepository;
    private final GoogleAccessTokens accessTokens;
    private final GoogleAnalyticsApiClient apiClient;
    private final ObjectMapper objectMapper;
    private final GoogleAnalyticsReports reports;

    public GoogleAnalyticsIntegrationProvider(IntegrationRepository integrationRepository,
            IntegrationSecretRepository integrationSecretRepository,
            GoogleAccessTokens accessTokens,
            GoogleAnalyticsApiClient apiClient,
            ObjectMapper objectMapper) {
        this.integrationRepository = integrationRepository;
        this.integrationSecretRepository = integrationSecretRepository;
        this.accessTokens = accessTokens;
        this.apiClient = apiClient;
        this.objectMapper = objectMapper;
        this.reports = new GoogleAnalyticsReports(objectMapper);
    }

    @Override
    public IntegrationType getType() {
        return IntegrationType.GOOGLE_ANALYTICS;
    }

    @Override
    @Transactional
    public Integration connect(UUID projectId, String displayName, Map<String, String> credentials) {
        if (projectId == null) {
            throw new IllegalArgumentException("projectId must not be null");
        }
        Map<String, String> c = credentials == null ? Map.of() : credentials;
        String propertyId = validatePropertyId(c.get("propertyId"));
        GoogleServiceAccount account = GoogleServiceAccount.parse(c.get("serviceAccountJson"), objectMapper);
        verify(propertyId, account);

        Integration integration = new Integration();
        integration.setDisplayName("Google Analytics");
        integration.setIntegrationType(IntegrationType.GOOGLE_ANALYTICS);
        integration.setProjectId(projectId);
        integration.setIntegrationStatus(IntegrationStatus.CONNECTED);
        integration = integrationRepository.save(integration);

        saveSecret(integration.getId(), payload(propertyId, account));
        return integration;
    }

    @Override
    @Transactional
    public Integration update(UUID projectId, Map<String, String> credentials) {
        Integration integration = findIntegration(projectId);
        GoogleAnalyticsSecretPayload current = readSecret(integration.getId());
        Map<String, String> c = credentials == null ? Map.of() : credentials;

        String propertyId = validatePropertyId(orCurrent(c.get("propertyId"), current.propertyId()));
        GoogleServiceAccount account = GoogleServiceAccount.parse(
                orCurrent(c.get("serviceAccountJson"), current.serviceAccountJson()), objectMapper);
        verify(propertyId, account);

        saveSecret(integration.getId(), payload(propertyId, account));
        return integration;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, String> getDisplayCredentials(UUID projectId) {
        GoogleAnalyticsSecretPayload payload = readSecret(findIntegration(projectId).getId());
        Map<String, String> result = new LinkedHashMap<>();
        result.put("propertyId", payload.propertyId());
        result.put("clientEmail", payload.clientEmail());
        result.put("serviceAccountJson", "****");
        return result;
    }

    /**
     * GA shows several IDs that look plausible. Name the one that was pasted
     * so the fix is obvious, rather than rejecting it as merely "invalid".
     */
    static String validatePropertyId(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("A GA4 Property ID is required.");
        }
        if (value.startsWith("properties/")) {
            value = value.substring("properties/".length());
        }
        if (value.toUpperCase().startsWith("G-")) {
            throw new IllegalArgumentException(
                    "That's a Measurement ID (G-...), which is used in your site's tracking tag. Threadspace needs "
                            + "the numeric Property ID from Admin -> Property details, like 123456789.");
        }
        if (value.toUpperCase().startsWith("UA-")) {
            throw new IllegalArgumentException(
                    "That's a Universal Analytics ID. Universal Analytics has been shut down - "
                            + "connect a GA4 property using its numeric Property ID instead.");
        }
        if (!PROPERTY_ID.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "The Property ID should be digits only, like 123456789. Find it under Admin -> Property details.");
        }
        return value;
    }

    /** Proves the key is valid and the account can read the property, before anything is saved. */
    private void verify(String propertyId, GoogleServiceAccount account) {
        String token;
        try {
            token = accessTokens.accessToken(account, GoogleAnalyticsApiClient.SCOPE);
        } catch (GoogleTokenException e) {
            throw new IllegalArgumentException(e.isRejected()
                    ? "Google rejected this service account key. It may have been deleted or disabled - "
                            + "create a new key for " + account.clientEmail() + " and try again."
                    : "Couldn't reach Google to verify the key. Check your connection and try again.", e);
        }

        try {
            apiClient.runReport(token, propertyId, reports.probeRequest());
        } catch (GoogleAnalyticsApiException e) {
            throw new IllegalArgumentException(describeFailure(e, propertyId, account), e);
        } catch (ResourceAccessException e) {
            throw new IllegalArgumentException(
                    "Couldn't reach Google Analytics to verify access. Check your connection and try again.", e);
        }
    }

    static String describeFailure(GoogleAnalyticsApiException e, String propertyId, GoogleServiceAccount account) {
        if (e.isApiDisabled()) {
            return "The Google Analytics Data API isn't enabled for Google Cloud project " + account.projectId()
                    + ". Enable it at https://console.cloud.google.com/apis/library/analyticsdata.googleapis.com?project="
                    + account.projectId() + ", wait a minute, then connect again.";
        }
        return switch (e.getStatusCode()) {
            case 403 -> "The service account " + account.clientEmail() + " can't read GA4 property " + propertyId
                    + ". In Google Analytics, open Admin -> Property access management, add that email, "
                    + "and give it the Viewer role.";
            case 400, 404 -> "Google Analytics didn't recognise property " + propertyId
                    + ". Check the Property ID under Admin -> Property details"
                    + (e.getApiMessage() != null ? " (" + e.getApiMessage() + ")." : ".");
            case 429 -> "Google Analytics is rate limiting this property right now. Wait a minute and try again.";
            default -> "Google Analytics returned an error while verifying access"
                    + (e.getApiMessage() != null ? ": " + e.getApiMessage() : " (HTTP " + e.getStatusCode() + ").");
        };
    }

    private static GoogleAnalyticsSecretPayload payload(String propertyId, GoogleServiceAccount account) {
        return new GoogleAnalyticsSecretPayload(propertyId, account.projectId(), account.clientEmail(), account.json());
    }

    private Integration findIntegration(UUID projectId) {
        return integrationRepository
                .findByProjectIdAndIntegrationType(projectId, IntegrationType.GOOGLE_ANALYTICS)
                .stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("No Google Analytics integration found for project"));
    }

    private void saveSecret(UUID integrationId, GoogleAnalyticsSecretPayload payload) {
        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize Google Analytics credentials", e);
        }
        IntegrationSecret secret = integrationSecretRepository
                .findByIntegrationId(integrationId)
                .orElseGet(IntegrationSecret::new);
        secret.setIntegrationId(integrationId);
        secret.setProvider(IntegrationType.GOOGLE_ANALYTICS);
        secret.setSecretJson(json);
        integrationSecretRepository.save(secret);
    }

    private GoogleAnalyticsSecretPayload readSecret(UUID integrationId) {
        IntegrationSecret secret = integrationSecretRepository
                .findByIntegrationId(integrationId)
                .orElseThrow(() -> new IllegalStateException("No secret found for Google Analytics integration"));
        try {
            return objectMapper.readValue(secret.getSecretJson(), GoogleAnalyticsSecretPayload.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to parse stored Google Analytics credentials", e);
        }
    }

    private static String orCurrent(String submitted, String current) {
        return submitted == null || submitted.isBlank() || submitted.startsWith("****") ? current : submitted;
    }
}
