package com.threadspace.backend.integration.stripe;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import com.fasterxml.jackson.databind.JsonNode;
import com.threadspace.backend.integration.stripe.StripeDtos.CurrencyAmount;
import com.threadspace.backend.integration.stripe.StripeDtos.CurrencyVolume;
import com.threadspace.backend.integration.stripe.StripeDtos.DailyRevenue;
import com.threadspace.backend.integration.stripe.StripeDtos.RecentCharge;

/**
 * Pure aggregation over raw Stripe objects. Kept free of I/O so the arithmetic -
 * the part most likely to be subtly wrong - can be unit tested directly.
 *
 * Amounts are summed per currency and never across currencies: adding yen to
 * dollars produces a number that means nothing.
 */
public final class StripeMetricsCalculator {

    private static final int RECENT_CHARGES = 10;

    private StripeMetricsCalculator() {
    }

    public record ChargeSummary(
            List<CurrencyVolume> volume,
            String primaryCurrency,
            List<DailyRevenue> dailyRevenue,
            int failedPayments,
            List<RecentCharge> recentCharges) {
    }

    public record MrrSummary(List<CurrencyAmount> mrr, int unpricedItems) {
    }

    /** Stripe balance entries ({@code available} / {@code pending}) in major units. */
    public static List<CurrencyAmount> balance(JsonNode entries) {
        List<CurrencyAmount> result = new ArrayList<>();
        for (JsonNode entry : entries) {
            String currency = currency(entry);
            result.add(new CurrencyAmount(currency, StripeMoney.toMajor(entry.path("amount").asLong(), currency)));
        }
        return result;
    }

    /**
     * Summarises charges created within {@code [start, end]} (UTC dates).
     *
     * A charge counts toward revenue only if it succeeded <em>and</em> was
     * captured - an authorised-but-uncaptured card hold is not money received.
     * Refunds are attributed to the day of the original charge.
     */
    public static ChargeSummary summarizeCharges(
            List<JsonNode> charges, LocalDate start, LocalDate end, String fallbackCurrency) {

        Map<String, long[]> byCurrency = new LinkedHashMap<>(); // gross, refunded, count
        Map<String, Map<LocalDate, long[]>> dailyByCurrency = new LinkedHashMap<>(); // net, count
        int failed = 0;

        for (JsonNode charge : charges) {
            String status = charge.path("status").asText();
            if ("failed".equals(status)) {
                failed++;
                continue;
            }
            if (!"succeeded".equals(status) || !charge.path("captured").asBoolean(false)) {
                continue;
            }

            String currency = currency(charge);
            long captured = charge.has("amount_captured")
                    ? charge.path("amount_captured").asLong()
                    : charge.path("amount").asLong();
            long refunded = charge.path("amount_refunded").asLong(0);

            long[] totals = byCurrency.computeIfAbsent(currency, c -> new long[3]);
            totals[0] += captured;
            totals[1] += refunded;
            totals[2]++;

            LocalDate day = utcDate(charge.path("created").asLong());
            long[] daily = dailyByCurrency
                    .computeIfAbsent(currency, c -> new TreeMap<>())
                    .computeIfAbsent(day, d -> new long[2]);
            daily[0] += captured - refunded;
            daily[1]++;
        }

        List<CurrencyVolume> volume = new ArrayList<>();
        for (Map.Entry<String, long[]> e : byCurrency.entrySet()) {
            String currency = e.getKey();
            long[] t = e.getValue();
            volume.add(new CurrencyVolume(
                    currency,
                    StripeMoney.toMajor(t[0], currency),
                    StripeMoney.toMajor(t[1], currency),
                    StripeMoney.toMajor(t[0] - t[1], currency),
                    (int) t[2]));
        }
        // Most-used currency first. Payment count, not amount, decides this:
        // comparing 1,000 JPY against 10 USD by magnitude would be meaningless.
        volume.sort(Comparator.comparingInt(CurrencyVolume::successfulPayments).reversed()
                .thenComparing(CurrencyVolume::currency));

        String primary = volume.isEmpty()
                ? normalizeCurrency(fallbackCurrency)
                : volume.get(0).currency();

        Map<LocalDate, long[]> primaryDaily = dailyByCurrency.getOrDefault(primary, Map.of());
        List<DailyRevenue> dailyRevenue = new ArrayList<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            long[] v = primaryDaily.getOrDefault(d, new long[2]);
            dailyRevenue.add(new DailyRevenue(d.toString(), StripeMoney.toMajor(v[0], primary), (int) v[1]));
        }

        List<RecentCharge> recent = charges.stream()
                .sorted(Comparator.comparingLong((JsonNode c) -> c.path("created").asLong()).reversed())
                .limit(RECENT_CHARGES)
                .map(c -> {
                    String currency = currency(c);
                    JsonNode description = c.get("description");
                    return new RecentCharge(
                            c.path("id").asText(),
                            StripeMoney.toMajor(c.path("amount").asLong(), currency),
                            currency,
                            c.path("status").asText(),
                            c.path("refunded").asBoolean(false),
                            c.path("created").asLong(),
                            description == null || description.isNull() ? null : description.asText());
                })
                .toList();

        return new ChargeSummary(volume, primary, dailyRevenue, failed, recent);
    }

    /**
     * Monthly recurring revenue from active subscriptions, per currency, before
     * discounts and tax. Every billing interval is normalised to a month: a
     * yearly $120 plan contributes $10, a weekly $10 plan contributes ~$43.33.
     *
     * Items without a fixed unit price (tiered or metered usage) can't be
     * expressed as a monthly amount without usage data, so they are counted in
     * {@code unpricedItems} instead of being guessed at.
     */
    public static MrrSummary monthlyRecurringRevenue(List<JsonNode> subscriptions) {
        Map<String, BigDecimal> minorByCurrency = new TreeMap<>();
        int unpriced = 0;

        for (JsonNode subscription : subscriptions) {
            for (JsonNode item : subscription.path("items").path("data")) {
                JsonNode price = item.path("price");
                BigDecimal unitAmount = unitAmount(price);
                JsonNode recurring = price.path("recurring");
                if (unitAmount == null || recurring.isMissingNode() || recurring.isNull()) {
                    unpriced++;
                    continue;
                }

                long quantity = item.has("quantity") && !item.path("quantity").isNull()
                        ? item.path("quantity").asLong()
                        : 1L;
                int intervalCount = Math.max(1, recurring.path("interval_count").asInt(1));

                BigDecimal monthly = monthlyFactor(recurring.path("interval").asText(), intervalCount);
                if (monthly == null) {
                    unpriced++;
                    continue;
                }

                String currency = currency(price);
                BigDecimal contribution = unitAmount
                        .multiply(BigDecimal.valueOf(quantity))
                        .multiply(monthly);
                minorByCurrency.merge(currency, contribution, BigDecimal::add);
            }
        }

        List<CurrencyAmount> mrr = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> e : minorByCurrency.entrySet()) {
            String currency = e.getKey();
            BigDecimal major = StripeMoney.toMajor(e.getValue(), currency)
                    .setScale(StripeMoney.decimals(currency), RoundingMode.HALF_UP);
            mrr.add(new CurrencyAmount(currency, major));
        }
        return new MrrSummary(mrr, unpriced);
    }

    /** How many of this interval fit in a month, as a multiplier on the per-interval price. */
    static BigDecimal monthlyFactor(String interval, int intervalCount) {
        BigDecimal count = BigDecimal.valueOf(intervalCount);
        BigDecimal twelve = BigDecimal.valueOf(12);
        return switch (interval) {
            case "month" -> BigDecimal.ONE.divide(count, 10, RoundingMode.HALF_UP);
            case "year" -> BigDecimal.ONE.divide(twelve.multiply(count), 10, RoundingMode.HALF_UP);
            case "week" -> BigDecimal.valueOf(52).divide(twelve.multiply(count), 10, RoundingMode.HALF_UP);
            case "day" -> BigDecimal.valueOf(365).divide(twelve.multiply(count), 10, RoundingMode.HALF_UP);
            default -> null;
        };
    }

    /**
     * Prefers {@code unit_amount_decimal}, which carries sub-cent precision for
     * prices like $0.005 that {@code unit_amount} would round away.
     */
    private static BigDecimal unitAmount(JsonNode price) {
        JsonNode decimal = price.get("unit_amount_decimal");
        if (decimal != null && !decimal.isNull() && !decimal.asText().isBlank()) {
            try {
                return new BigDecimal(decimal.asText());
            } catch (NumberFormatException ignored) {
                // fall through to unit_amount
            }
        }
        JsonNode amount = price.get("unit_amount");
        return amount == null || amount.isNull() ? null : BigDecimal.valueOf(amount.asLong());
    }

    private static String currency(JsonNode node) {
        return normalizeCurrency(node.path("currency").asText(""));
    }

    private static String normalizeCurrency(String currency) {
        return currency == null || currency.isBlank() ? "usd" : currency.toLowerCase(Locale.ROOT);
    }

    private static LocalDate utcDate(long epochSeconds) {
        return Instant.ofEpochSecond(epochSeconds).atZone(ZoneOffset.UTC).toLocalDate();
    }
}
