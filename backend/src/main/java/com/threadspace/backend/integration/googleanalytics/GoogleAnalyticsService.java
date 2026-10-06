package com.threadspace.backend.integration.googleanalytics;

import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.threadspace.backend.integration.core.IntegrationCredentials;
import com.threadspace.backend.integration.core.IntegrationType;
import com.threadspace.backend.integration.google.GoogleAccessTokens;
import com.threadspace.backend.integration.google.GoogleServiceAccount;
import com.threadspace.backend.integration.googleanalytics.GoogleAnalyticsDtos.GoogleAnalyticsMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class GoogleAnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(GoogleAnalyticsService.class);

    static final int DEFAULT_DAYS = 28;
    static final int MAX_DAYS = 90;

    private final IntegrationCredentials credentials;
    private final GoogleAccessTokens accessTokens;
    private final GoogleAnalyticsApiClient apiClient;
    private final ObjectMapper objectMapper;
    private final GoogleAnalyticsReports reports;

    public GoogleAnalyticsService(IntegrationCredentials credentials,
            GoogleAccessTokens accessTokens,
            GoogleAnalyticsApiClient apiClient,
            ObjectMapper objectMapper) {
        this.credentials = credentials;
        this.accessTokens = accessTokens;
        this.apiClient = apiClient;
        this.objectMapper = objectMapper;
        this.reports = new GoogleAnalyticsReports(objectMapper);
    }

    public GoogleAnalyticsMetrics getMetrics(UUID projectId, int days) {
        GoogleAnalyticsSecretPayload payload = credentials.loadConnected(
                projectId, IntegrationType.GOOGLE_ANALYTICS, GoogleAnalyticsSecretPayload.class, "Google Analytics");
        int window = Math.max(1, Math.min(days, MAX_DAYS));

        GoogleServiceAccount account = GoogleServiceAccount.parse(payload.serviceAccountJson(), objectMapper);
        String token = accessTokens.accessToken(account, GoogleAnalyticsApiClient.SCOPE);

        JsonNode batch = apiClient.batchRunReports(token, payload.propertyId(), reports.overviewRequests(window));

        // "Active right now" is a nicety; a failure here shouldn't blank the page.
        Long activeNow = null;
        try {
            activeNow = GoogleAnalyticsReports.activeUsersNow(
                    apiClient.runRealtimeReport(token, payload.propertyId(), reports.realtimeRequest()));
        } catch (GoogleAnalyticsApiException e) {
            log.warn("Realtime report failed for property {}: {}", payload.propertyId(), e.getMessage());
        }

        String timeZone = batch.path("reports").path(0).path("metadata").path("timeZone").asText("");
        return reports.toMetrics(payload.propertyId(), window, batch, activeNow,
                GoogleAnalyticsReports.today(timeZone));
    }
}
