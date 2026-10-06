package com.threadspace.backend.integration.stripe;

import java.math.BigDecimal;
import java.util.List;

/** Response shapes for the Stripe metrics endpoint. All amounts are in major units. */
public final class StripeDtos {

    private StripeDtos() {
    }

    public record CurrencyAmount(String currency, BigDecimal amount) {
    }

    /** Payment volume for one currency over the reporting window. */
    public record CurrencyVolume(
            String currency,
            BigDecimal gross,
            BigDecimal refunded,
            BigDecimal net,
            int successfulPayments) {
    }

    /** Net revenue for a single UTC day, in the primary currency. */
    public record DailyRevenue(String date, BigDecimal amount, int payments) {
    }

    public record RecentCharge(
            String id,
            BigDecimal amount,
            String currency,
            String status,
            boolean refunded,
            long created,
            String description) {
    }

    public record StripeMetrics(
            boolean livemode,
            int periodDays,
            String primaryCurrency,
            List<CurrencyAmount> availableBalance,
            List<CurrencyAmount> pendingBalance,
            List<CurrencyVolume> volume,
            List<DailyRevenue> dailyRevenue,
            int activeSubscriptions,
            List<CurrencyAmount> mrr,
            int unpricedSubscriptionItems,
            int newCustomers,
            int failedPayments,
            List<RecentCharge> recentCharges,
            boolean truncated) {
    }
}
