package com.threadspace.backend.integration.googleanalytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.threadspace.backend.integration.core.Integration;
import com.threadspace.backend.integration.core.IntegrationCredentials;
import com.threadspace.backend.integration.core.IntegrationRepository;
import com.threadspace.backend.integration.core.IntegrationSecret;
import com.threadspace.backend.integration.core.IntegrationSecretRepository;
import com.threadspace.backend.integration.core.IntegrationStatus;
import com.threadspace.backend.integration.core.IntegrationType;
import com.threadspace.backend.integration.google.GoogleAccessTokens;
import com.threadspace.backend.integration.googleanalytics.GoogleAnalyticsDtos.GoogleAnalyticsMetrics;
import com.threadspace.backend.integration.testsupport.StubServer;
import com.threadspace.backend.integration.testsupport.StubServer.Response;
import com.threadspace.backend.integration.testsupport.TestKeys;
import org.junit.jupiter.api.Test;

/**
 * The whole backend path except the database: stored key, real OAuth token
 * exchange, real HTTP client, and parsing - against one local server playing
 * both Google's token endpoint and the Data API.
 */
class GoogleAnalyticsServiceTest {

    private static final UUID PROJECT = UUID.randomUUID();
    private static final String TIME_ZONE = "America/New_York";

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void exchangesTheKeyForATokenThenReadsEveryReport() throws Exception {
        LocalDate today = LocalDate.now(ZoneId.of(TIME_ZONE));
        try (StubServer google = new StubServer(req -> switch (req.path()) {
            case "/token" -> Response.json(200,
                    "{\"access_token\":\"ya29.service-test\",\"expires_in\":3599,\"token_type\":\"Bearer\"}");
            case "/v1beta/properties/123456789:batchRunReports" ->
                    Response.json(200, GoogleAnalyticsFixtures.batchResponse(today, TIME_ZONE));
            case "/v1beta/properties/123456789:runRealtimeReport" ->
                    Response.json(200, GoogleAnalyticsFixtures.REALTIME_RESPONSE);
            default -> Response.json(404, "{}");
        })) {
            GoogleAnalyticsService service = service(google);

            GoogleAnalyticsMetrics metrics = service.getMetrics(PROJECT, 28);

            assertThat(metrics.propertyId()).isEqualTo("123456789");
            assertThat(metrics.periodDays()).isEqualTo(28);
            assertThat(metrics.timeZone()).isEqualTo(TIME_ZONE);
            assertThat(metrics.totals().sessions()).isEqualTo(2104);
            assertThat(metrics.activeUsersNow()).isEqualTo(7);
            assertThat(metrics.daily()).hasSize(28);
            // The series ends on the property's today, not the server's.
            assertThat(metrics.daily().get(27).date()).isEqualTo(today.toString());
            assertThat(metrics.daily().get(27).activeUsers()).isEqualTo(61);

            // Token first, then data calls carrying it.
            assertThat(google.requests().get(0).path()).isEqualTo("/token");
            assertThat(google.requests()).filteredOn(r -> r.path().startsWith("/v1beta"))
                    .hasSize(2)
                    .allSatisfy(r -> assertThat(r.header("Authorization")).isEqualTo("Bearer ya29.service-test"));
        }
    }

    @Test
    void aRealtimeFailureDoesntBlankThePage() throws Exception {
        LocalDate today = LocalDate.now(ZoneId.of(TIME_ZONE));
        try (StubServer google = new StubServer(req -> switch (req.path()) {
            case "/token" -> Response.json(200, "{\"access_token\":\"t\",\"expires_in\":3599,\"token_type\":\"Bearer\"}");
            case "/v1beta/properties/123456789:batchRunReports" ->
                    Response.json(200, GoogleAnalyticsFixtures.batchResponse(today, TIME_ZONE));
            default -> Response.json(500, "{\"error\":{\"code\":500,\"status\":\"INTERNAL\"}}");
        })) {
            GoogleAnalyticsMetrics metrics = service(google).getMetrics(PROJECT, 7);

            assertThat(metrics.activeUsersNow()).isNull();
            assertThat(metrics.totals().activeUsers()).isEqualTo(1520);
        }
    }

    private GoogleAnalyticsService service(StubServer google) throws Exception {
        Integration integration = new Integration();
        integration.setId(UUID.randomUUID());
        integration.setIntegrationStatus(IntegrationStatus.CONNECTED);
        IntegrationRepository integrations = mock(IntegrationRepository.class);
        when(integrations.findByProjectIdAndIntegrationType(PROJECT, IntegrationType.GOOGLE_ANALYTICS))
                .thenReturn(List.of(integration));

        String keyJson = TestKeys.serviceAccountJson("my-project",
                "threadspace@my-project.iam.gserviceaccount.com", "https://oauth2.googleapis.com/token");
        IntegrationSecret secret = new IntegrationSecret();
        secret.setSecretJson(mapper.writeValueAsString(new GoogleAnalyticsSecretPayload(
                "123456789", "my-project", "threadspace@my-project.iam.gserviceaccount.com", keyJson)));
        IntegrationSecretRepository secrets = mock(IntegrationSecretRepository.class);
        when(secrets.findByIntegrationId(any())).thenReturn(Optional.of(secret));

        return new GoogleAnalyticsService(new IntegrationCredentials(integrations, secrets, mapper),
                new GoogleAccessTokens(google.baseUrl() + "/token"),
                new GoogleAnalyticsApiClient(google.baseUrl(), mapper), mapper);
    }
}
