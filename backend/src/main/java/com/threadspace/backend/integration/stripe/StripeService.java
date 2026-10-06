package com.threadspace.backend.integration.stripe;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.threadspace.backend.integration.core.IntegrationCredentials;
import com.threadspace.backend.integration.core.IntegrationType;
import com.threadspace.backend.integration.stripe.StripeApiClient.ListResult;
import com.threadspace.backend.integration.stripe.StripeDtos.CurrencyAmount;
import com.threadspace.backend.integration.stripe.StripeDtos.StripeMetrics;
import org.springframework.stereotype.Service;

@Service
public class StripeService {

    /**
     * Upper bound on objects fetched per list. Keeps a busy account from turning
     * one page load into hundreds of API calls; the response flags truncation.
     */
    static final int MAX_ITEMS = 1000;

    static final int DEFAULT_DAYS = 30;
    static final int MAX_DAYS = 90;

    private final IntegrationCredentials credentials;
    private final StripeApiClient stripeApiClient;

    public StripeService(IntegrationCredentials credentials, StripeApiClient stripeApiClient) {
        this.credentials = credentials;
        this.stripeApiClient = stripeApiClient;
    }

    public StripeMetrics getMetrics(UUID projectId, int days) {
        StripeSecretPayload stored = credentials.loadConnected(
                projectId, IntegrationType.STRIPE, StripeSecretPayload.class, "Stripe");
        String key = stored.apiKey();

        int window = Math.max(1, Math.min(days, MAX_DAYS));
        LocalDate end = LocalDate.now(ZoneOffset.UTC);
        LocalDate start = end.minusDays(window - 1L);
        String since = String.valueOf(start.atStartOfDay(ZoneOffset.UTC).toEpochSecond());

        JsonNode balance = stripeApiClient.get(key, "/v1/balance", Map.of());
        ListResult charges = stripeApiClient.list(key, "/v1/charges", Map.of("created[gte]", since), MAX_ITEMS);
        ListResult subscriptions = stripeApiClient.list(key, "/v1/subscriptions", Map.of("status", "active"),
                MAX_ITEMS);
        ListResult customers = stripeApiClient.list(key, "/v1/customers", Map.of("created[gte]", since),
                MAX_ITEMS);

        List<CurrencyAmount> available = StripeMetricsCalculator.balance(balance.path("available"));
        List<CurrencyAmount> pending = StripeMetricsCalculator.balance(balance.path("pending"));
        String fallbackCurrency = available.isEmpty() ? "usd" : available.get(0).currency();

        StripeMetricsCalculator.ChargeSummary chargeSummary = StripeMetricsCalculator.summarizeCharges(
                charges.items(), start, end, fallbackCurrency);
        StripeMetricsCalculator.MrrSummary mrr = StripeMetricsCalculator.monthlyRecurringRevenue(
                subscriptions.items());

        return new StripeMetrics(
                stored.livemode(),
                window,
                chargeSummary.primaryCurrency(),
                available,
                pending,
                chargeSummary.volume(),
                chargeSummary.dailyRevenue(),
                subscriptions.items().size(),
                mrr.mrr(),
                mrr.unpricedItems(),
                customers.items().size(),
                chargeSummary.failedPayments(),
                chargeSummary.recentCharges(),
                charges.truncated() || subscriptions.truncated() || customers.truncated());
    }
}
