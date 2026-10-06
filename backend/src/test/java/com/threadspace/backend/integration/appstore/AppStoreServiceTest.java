package com.threadspace.backend.integration.appstore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.threadspace.backend.integration.appstore.AppStoreDtos.AppStoreMetrics;
import com.threadspace.backend.integration.core.Integration;
import com.threadspace.backend.integration.core.IntegrationCredentials;
import com.threadspace.backend.integration.core.IntegrationRepository;
import com.threadspace.backend.integration.core.IntegrationSecret;
import com.threadspace.backend.integration.core.IntegrationSecretRepository;
import com.threadspace.backend.integration.core.IntegrationStatus;
import com.threadspace.backend.integration.core.IntegrationType;
import com.threadspace.backend.integration.testsupport.StubServer;
import com.threadspace.backend.integration.testsupport.StubServer.Response;
import com.threadspace.backend.integration.testsupport.TestKeys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * The whole backend path except the database: stored credentials, JWT signing,
 * concurrent report downloads, gzip parsing and aggregation, against a local
 * server shaped like App Store Connect.
 */
class AppStoreServiceTest {

    private static final UUID PROJECT = UUID.randomUUID();
    private static final String KEY_ID = "2X9R4HXF34";
    private static final String VENDOR = "85012345";

    private final ObjectMapper mapper = new ObjectMapper();
    private final String pem = TestKeys.pkcs8Pem(TestKeys.ecKeyPair("secp256r1"));
    private AppStoreService service;

    @AfterEach
    void tearDown() {
        if (service != null) {
            service.shutdown();
        }
    }

    @Test
    void downloadsOneReportPerDayAndTreatsMissingDaysAsZero() throws Exception {
        LocalDate end = LocalDate.now(AppStoreService.REPORT_ZONE).minusDays(1);
        byte[] report = TestKeys.gzip(AppStoreSalesReportTest.sampleReport().getBytes(StandardCharsets.UTF_8));

        try (StubServer apple = new StubServer(req -> {
            if (req.path().equals("/v1/apps")) {
                return Response.json(200, """
                        {"data":[{"id":"1111","type":"apps",
                                  "attributes":{"name":"Habit Tracker","bundleId":"com.example.habits"}}]}""");
            }
            // The most recent day hasn't been published yet.
            if (req.query().get("filter[reportDate]").equals(end.toString())) {
                return Response.json(404, "{\"errors\":[{\"status\":\"404\",\"code\":\"NOT_FOUND\"}]}");
            }
            return Response.bytes(200, "application/a-gzip", report);
        })) {
            service = newService(apple.baseUrl());

            AppStoreMetrics metrics = service.getMetrics(PROJECT, 3);

            assertThat(metrics.periodDays()).isEqualTo(3);
            assertThat(metrics.daily()).extracting(AppStoreDtos.DailyUnits::date).containsExactly(
                    end.minusDays(2).toString(), end.minusDays(1).toString(), end.toString());
            assertThat(metrics.daysWithoutReport()).isEqualTo(1);
            assertThat(metrics.latestReportDate()).isEqualTo(end.minusDays(1).toString());

            // Two published days, each with the sample report's 12 downloads.
            assertThat(metrics.totalDownloads()).isEqualTo(24);
            assertThat(metrics.totalInAppPurchases()).isEqualTo(4);
            assertThat(metrics.apps()).extracting(AppStoreDtos.AppInfo::bundleId).containsExactly("com.example.habits");

            // Every request carried a signed token naming this key.
            assertThat(apple.requests()).hasSize(4).allSatisfy(r -> {
                String token = r.header("Authorization").substring("Bearer ".length());
                JsonNode header = mapper.readTree(Base64.getUrlDecoder().decode(token.split("\\.")[0]));
                assertThat(header.path("kid").asText()).isEqualTo(KEY_ID);
            });
            assertThat(apple.requests()).filteredOn(r -> r.path().equals("/v1/salesReports"))
                    .allSatisfy(r -> assertThat(r.query()).containsEntry("filter[vendorNumber]", VENDOR));
        }
    }

    @Test
    void aForbiddenReportSurfacesAsTheApiErrorNotAWrapper() throws Exception {
        try (StubServer apple = new StubServer(req -> req.path().equals("/v1/apps")
                ? Response.json(200, "{\"data\":[]}")
                : Response.json(403, "{\"errors\":[{\"status\":\"403\",\"code\":\"FORBIDDEN_ERROR\"}]}"))) {
            service = newService(apple.baseUrl());

            assertThatThrownBy(() -> service.getMetrics(PROJECT, 2))
                    .isInstanceOfSatisfying(AppStoreApiException.class,
                            e -> assertThat(e.getStatusCode()).isEqualTo(403));
        }
    }

    private AppStoreService newService(String baseUrl) throws Exception {
        Integration integration = new Integration();
        integration.setId(UUID.randomUUID());
        integration.setIntegrationStatus(IntegrationStatus.CONNECTED);
        IntegrationRepository integrations = mock(IntegrationRepository.class);
        when(integrations.findByProjectIdAndIntegrationType(PROJECT, IntegrationType.APPLE_CONNECT))
                .thenReturn(List.of(integration));

        IntegrationSecret secret = new IntegrationSecret();
        secret.setSecretJson(mapper.writeValueAsString(new AppStoreSecretPayload(
                "57246542-96fe-1a63-e053-0824d011072a", KEY_ID, pem, VENDOR)));
        IntegrationSecretRepository secrets = mock(IntegrationSecretRepository.class);
        when(secrets.findByIntegrationId(any())).thenReturn(Optional.of(secret));

        return new AppStoreService(new IntegrationCredentials(integrations, secrets, mapper),
                new AppStoreApiClient(baseUrl, mapper));
    }
}
