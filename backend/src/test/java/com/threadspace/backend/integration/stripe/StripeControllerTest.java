package com.threadspace.backend.integration.stripe;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.threadspace.backend.integration.stripe.StripeDtos.CurrencyAmount;
import com.threadspace.backend.integration.stripe.StripeDtos.CurrencyVolume;
import com.threadspace.backend.integration.stripe.StripeDtos.DailyRevenue;
import com.threadspace.backend.integration.stripe.StripeDtos.RecentCharge;
import com.threadspace.backend.integration.stripe.StripeDtos.StripeMetrics;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Pins the JSON contract the Stripe revenue page reads, and the mapping from
 * Stripe failures to the messages the page displays.
 */
@WebMvcTest(StripeController.class)
@TestPropertySource(properties = "INTERNAL_SYNC_TOKEN=test-token")
class StripeControllerTest {

    private static final UUID PROJECT = UUID.randomUUID();

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private StripeService service;

    @Test
    void rejectsRequestsWithoutTheInternalToken() throws Exception {
        mvc.perform(get("/projects/{id}/stripe/metrics", PROJECT)).andExpect(status().isUnauthorized());
        mvc.perform(get("/projects/{id}/stripe/metrics", PROJECT).header("x-internal-token", "wrong"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void servesTheFieldsTheRevenuePageReadsAsNumbers() throws Exception {
        when(service.getMetrics(eq(PROJECT), anyInt())).thenReturn(new StripeMetrics(
                false, 7, "usd",
                List.of(new CurrencyAmount("usd", new BigDecimal("2500.00"))),
                List.of(new CurrencyAmount("usd", new BigDecimal("50.00"))),
                List.of(new CurrencyVolume("usd", new BigDecimal("70.00"), new BigDecimal("10.00"),
                        new BigDecimal("60.00"), 2)),
                List.of(new DailyRevenue("2026-09-27", new BigDecimal("40.00"), 1)),
                2, List.of(new CurrencyAmount("usd", new BigDecimal("49.00"))), 1, 3, 1,
                List.of(new RecentCharge("ch_1", new BigDecimal("50.00"), "usd", "succeeded", false,
                        1790000000L, null)),
                false));

        mvc.perform(get("/projects/{id}/stripe/metrics", PROJECT)
                .param("days", "7")
                .header("x-internal-token", "test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.livemode").value(false))
                .andExpect(jsonPath("$.periodDays").value(7))
                .andExpect(jsonPath("$.primaryCurrency").value("usd"))
                // Money must arrive as JSON numbers - the page does arithmetic on them.
                .andExpect(jsonPath("$.volume[0].net").value(Matchers.closeTo(60.0, 0.001)))
                .andExpect(jsonPath("$.volume[0].gross").isNumber())
                .andExpect(jsonPath("$.volume[0].refunded").isNumber())
                .andExpect(jsonPath("$.volume[0].successfulPayments").value(2))
                .andExpect(jsonPath("$.availableBalance[0].amount").isNumber())
                .andExpect(jsonPath("$.pendingBalance[0].currency").value("usd"))
                .andExpect(jsonPath("$.dailyRevenue[0].date").value("2026-09-27"))
                .andExpect(jsonPath("$.dailyRevenue[0].amount").isNumber())
                .andExpect(jsonPath("$.mrr[0].amount").isNumber())
                .andExpect(jsonPath("$.activeSubscriptions").value(2))
                .andExpect(jsonPath("$.unpricedSubscriptionItems").value(1))
                .andExpect(jsonPath("$.newCustomers").value(3))
                .andExpect(jsonPath("$.failedPayments").value(1))
                .andExpect(jsonPath("$.recentCharges[0].created").isNumber())
                .andExpect(jsonPath("$.recentCharges[0].refunded").value(false))
                .andExpect(jsonPath("$.truncated").value(false));

        verify(service).getMetrics(PROJECT, 7);
    }

    @Test
    void aRevokedKeyBecomesAnActionableMessage() throws Exception {
        when(service.getMetrics(any(), anyInt()))
                .thenThrow(new StripeApiException(401, "invalid_request_error", null, "Invalid API Key", null));

        mvc.perform(get("/projects/{id}/stripe/metrics", PROJECT).header("x-internal-token", "test-token"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(Matchers.containsString("reconnect with a new key")));
    }

    @Test
    void aMissingIntegrationIsNotFound() throws Exception {
        when(service.getMetrics(any(), anyInt()))
                .thenThrow(new IllegalArgumentException("No connected Stripe integration found for project"));

        mvc.perform(get("/projects/{id}/stripe/metrics", PROJECT).header("x-internal-token", "test-token"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(Matchers.containsString("No connected Stripe integration")));
    }

    @Test
    void rateLimitingIsReportedAsTemporary() throws Exception {
        when(service.getMetrics(any(), anyInt()))
                .thenThrow(new StripeApiException(429, "rate_limit_error", null, "Too many requests", null));

        mvc.perform(get("/projects/{id}/stripe/metrics", PROJECT).header("x-internal-token", "test-token"))
                .andExpect(status().isServiceUnavailable());
    }
}
