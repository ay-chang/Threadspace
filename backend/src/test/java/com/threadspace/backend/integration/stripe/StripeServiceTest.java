package com.threadspace.backend.integration.stripe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
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
import com.threadspace.backend.integration.stripe.StripeDtos.StripeMetrics;
import com.threadspace.backend.integration.testsupport.StubServer;
import com.threadspace.backend.integration.testsupport.StubServer.Response;
import org.junit.jupiter.api.Test;

/**
 * The whole backend path except the database: stored credentials, the real
 * HTTP client, pagination and aggregation, against a local server that answers
 * with Stripe-shaped JSON.
 */
class StripeServiceTest {

    private static final UUID PROJECT = UUID.randomUUID();
    private static final String KEY = "rk_live_serviceTestKey1234";

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void assemblesMetricsFromEveryEndpoint() throws Exception {
        long yesterday = Instant.now().minus(1, ChronoUnit.DAYS).getEpochSecond();
        long today = Instant.now().getEpochSecond();

        try (StubServer stripe = new StubServer(req -> switch (req.path()) {
            case "/v1/balance" -> Response.json(200, """
                    {"object":"balance",
                     "available":[{"amount":250000,"currency":"usd"}],
                     "pending":[{"amount":5000,"currency":"usd"}]}""");
            case "/v1/charges" -> req.query().containsKey("starting_after")
                    ? Response.json(200, """
                            {"object":"list","has_more":false,"data":[
                              {"id":"ch_3","status":"succeeded","captured":true,"amount":1000,
                               "amount_captured":1000,"amount_refunded":0,"currency":"usd","created":%d}]}"""
                            .formatted(yesterday))
                    : Response.json(200, """
                            {"object":"list","has_more":true,"data":[
                              {"id":"ch_1","status":"succeeded","captured":true,"amount":5000,
                               "amount_captured":5000,"amount_refunded":1000,"currency":"usd","created":%d},
                              {"id":"ch_2","status":"failed","captured":false,"amount":2000,
                               "amount_captured":0,"amount_refunded":0,"currency":"usd","created":%d}]}"""
                            .formatted(today, today));
            case "/v1/subscriptions" -> Response.json(200, """
                    {"object":"list","has_more":false,"data":[
                      {"id":"sub_1","status":"active","items":{"data":[{"quantity":1,
                        "price":{"currency":"usd","unit_amount":2900,"unit_amount_decimal":"2900",
                                 "recurring":{"interval":"month","interval_count":1}}}]}},
                      {"id":"sub_2","status":"active","items":{"data":[{"quantity":1,
                        "price":{"currency":"usd","unit_amount":24000,"unit_amount_decimal":"24000",
                                 "recurring":{"interval":"year","interval_count":1}}}]}}]}""");
            case "/v1/customers" -> Response.json(200, """
                    {"object":"list","has_more":false,"data":[{"id":"cus_1"},{"id":"cus_2"},{"id":"cus_3"}]}""");
            default -> Response.json(404, "{\"error\":{\"message\":\"Unrecognized request URL\"}}");
        })) {
            StripeService service = new StripeService(
                    new IntegrationCredentials(connectedIntegration(), storedKey(), mapper),
                    new StripeApiClient(stripe.baseUrl(), mapper));

            StripeMetrics metrics = service.getMetrics(PROJECT, 30);

            assertThat(metrics.livemode()).isTrue();
            assertThat(metrics.periodDays()).isEqualTo(30);
            assertThat(metrics.primaryCurrency()).isEqualTo("usd");
            assertThat(metrics.availableBalance().get(0).amount()).isEqualByComparingTo("2500.00");
            assertThat(metrics.pendingBalance().get(0).amount()).isEqualByComparingTo("50.00");

            // ch_1 (50 - 10 refunded) + ch_3 (10); ch_2 failed.
            assertThat(metrics.volume().get(0).net()).isEqualByComparingTo("50.00");
            assertThat(metrics.volume().get(0).successfulPayments()).isEqualTo(2);
            assertThat(metrics.failedPayments()).isEqualTo(1);

            // $29/mo + $240/yr ($20/mo).
            assertThat(metrics.activeSubscriptions()).isEqualTo(2);
            assertThat(metrics.mrr().get(0).amount()).isEqualByComparingTo("49.00");
            assertThat(metrics.newCustomers()).isEqualTo(3);

            assertThat(metrics.dailyRevenue()).hasSize(30);
            assertThat(metrics.dailyRevenue().get(29).date()).isEqualTo(LocalDate.now(ZoneOffset.UTC).toString());
            assertThat(metrics.recentCharges()).extracting(StripeDtos.RecentCharge::id).first().isIn("ch_1", "ch_2");
            assertThat(metrics.truncated()).isFalse();

            // Every call used the stored key; the window and status filters went out.
            assertThat(stripe.requests()).allSatisfy(r -> assertThat(r.header("Authorization")).isEqualTo("Bearer " + KEY));
            long windowStart = LocalDate.now(ZoneOffset.UTC).minusDays(29).atStartOfDay(ZoneOffset.UTC).toEpochSecond();
            assertThat(stripe.requests()).filteredOn(r -> r.path().equals("/v1/charges"))
                    .hasSize(2)
                    .allSatisfy(r -> assertThat(r.query()).containsEntry("created[gte]", String.valueOf(windowStart)));
            assertThat(stripe.requests()).filteredOn(r -> r.path().equals("/v1/subscriptions"))
                    .allSatisfy(r -> assertThat(r.query()).containsEntry("status", "active"));
        }
    }

    @Test
    void periodIsClampedToTheSupportedRange() throws Exception {
        try (StubServer stripe = new StubServer(req -> req.path().equals("/v1/balance")
                ? Response.json(200, "{\"available\":[],\"pending\":[]}")
                : Response.json(200, "{\"has_more\":false,\"data\":[]}"))) {
            StripeService service = new StripeService(
                    new IntegrationCredentials(connectedIntegration(), storedKey(), mapper),
                    new StripeApiClient(stripe.baseUrl(), mapper));

            assertThat(service.getMetrics(PROJECT, 5000).periodDays()).isEqualTo(StripeService.MAX_DAYS);
            assertThat(service.getMetrics(PROJECT, 0).periodDays()).isEqualTo(1);
        }
    }

    private IntegrationRepository connectedIntegration() {
        Integration integration = new Integration();
        integration.setId(UUID.randomUUID());
        integration.setIntegrationStatus(IntegrationStatus.CONNECTED);
        IntegrationRepository repository = mock(IntegrationRepository.class);
        when(repository.findByProjectIdAndIntegrationType(PROJECT, IntegrationType.STRIPE))
                .thenReturn(List.of(integration));
        return repository;
    }

    private IntegrationSecretRepository storedKey() throws Exception {
        IntegrationSecret secret = new IntegrationSecret();
        secret.setSecretJson(mapper.writeValueAsString(new StripeSecretPayload(KEY, true)));
        IntegrationSecretRepository repository = mock(IntegrationSecretRepository.class);
        when(repository.findByIntegrationId(org.mockito.ArgumentMatchers.any())).thenReturn(Optional.of(secret));
        return repository;
    }
}
