package com.threadspace.backend.integration.stripe;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.threadspace.backend.integration.stripe.StripeDtos.CurrencyAmount;
import com.threadspace.backend.integration.stripe.StripeDtos.CurrencyVolume;
import com.threadspace.backend.integration.stripe.StripeMetricsCalculator.ChargeSummary;
import com.threadspace.backend.integration.stripe.StripeMetricsCalculator.MrrSummary;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class StripeMetricsCalculatorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final LocalDate DAY1 = LocalDate.of(2026, 9, 1);
    private static final LocalDate DAY2 = DAY1.plusDays(1);
    private static final LocalDate DAY3 = DAY1.plusDays(2);

    @Nested
    class Money {

        @Test
        void twoDecimalCurrencies() {
            assertThat(StripeMoney.toMajor(1999, "usd")).isEqualByComparingTo("19.99");
            assertThat(StripeMoney.toMajor(1999, "EUR")).isEqualByComparingTo("19.99");
        }

        @Test
        void zeroDecimalCurrenciesAreNotDividedByOneHundred() {
            assertThat(StripeMoney.toMajor(1000, "jpy")).isEqualByComparingTo("1000");
            assertThat(StripeMoney.toMajor(50000, "krw")).isEqualByComparingTo("50000");
        }

        @Test
        void threeDecimalCurrencies() {
            assertThat(StripeMoney.toMajor(1234, "kwd")).isEqualByComparingTo("1.234");
        }
    }

    @Nested
    class Charges {

        @Test
        void countsOnlySucceededCapturedChargesPerCurrency() {
            List<JsonNode> charges = List.of(
                    charge("ch_1", "succeeded", true, 5000, 1000, "usd", DAY1),
                    charge("ch_2", "succeeded", true, 2000, 0, "usd", DAY2),
                    charge("ch_3", "failed", false, 3000, 0, "usd", DAY2),
                    charge("ch_4", "succeeded", false, 9999, 0, "usd", DAY2), // authorised, never captured
                    charge("ch_5", "pending", false, 4000, 0, "usd", DAY3),
                    charge("ch_6", "succeeded", true, 1000, 0, "jpy", DAY1));

            ChargeSummary summary = StripeMetricsCalculator.summarizeCharges(charges, DAY1, DAY3, "usd");

            CurrencyVolume usd = volume(summary, "usd");
            assertThat(usd.gross()).isEqualByComparingTo("70.00");
            assertThat(usd.refunded()).isEqualByComparingTo("10.00");
            assertThat(usd.net()).isEqualByComparingTo("60.00");
            assertThat(usd.successfulPayments()).isEqualTo(2);

            CurrencyVolume jpy = volume(summary, "jpy");
            assertThat(jpy.net()).isEqualByComparingTo("1000");
            assertThat(jpy.successfulPayments()).isEqualTo(1);

            assertThat(summary.failedPayments()).isEqualTo(1);
            assertThat(summary.primaryCurrency()).isEqualTo("usd");
        }

        @Test
        void usesAmountCapturedForPartialCaptures() {
            ObjectNode partial = charge("ch_1", "succeeded", true, 5000, 0, "usd", DAY1);
            partial.put("amount_captured", 3000);

            ChargeSummary summary = StripeMetricsCalculator.summarizeCharges(List.of(partial), DAY1, DAY1, "usd");

            assertThat(volume(summary, "usd").gross()).isEqualByComparingTo("30.00");
        }

        @Test
        void dailySeriesCoversEveryDayIncludingZeroDays() {
            List<JsonNode> charges = List.of(
                    charge("ch_1", "succeeded", true, 5000, 1000, "usd", DAY1),
                    charge("ch_2", "succeeded", true, 2000, 0, "usd", DAY2));

            ChargeSummary summary = StripeMetricsCalculator.summarizeCharges(charges, DAY1, DAY3, "usd");

            assertThat(summary.dailyRevenue()).extracting(StripeDtos.DailyRevenue::date)
                    .containsExactly("2026-09-01", "2026-09-02", "2026-09-03");
            assertThat(summary.dailyRevenue()).extracting(StripeDtos.DailyRevenue::amount)
                    .usingElementComparator(BigDecimal::compareTo)
                    .containsExactly(new BigDecimal("40.00"), new BigDecimal("20.00"), BigDecimal.ZERO);
        }

        @Test
        void primaryCurrencyIsTheMostUsedNotTheLargestNumber() {
            // One $1,000 payment against three 100-yen payments. Comparing 100000
            // cents to 300 yen by magnitude would pick USD; by usage it's JPY.
            List<JsonNode> charges = List.of(
                    charge("ch_1", "succeeded", true, 100000, 0, "usd", DAY1),
                    charge("ch_2", "succeeded", true, 100, 0, "jpy", DAY1),
                    charge("ch_3", "succeeded", true, 100, 0, "jpy", DAY1),
                    charge("ch_4", "succeeded", true, 100, 0, "jpy", DAY1));

            ChargeSummary summary = StripeMetricsCalculator.summarizeCharges(charges, DAY1, DAY1, "usd");

            assertThat(summary.primaryCurrency()).isEqualTo("jpy");
            assertThat(summary.volume().get(0).currency()).isEqualTo("jpy");
        }

        @Test
        void noChargesFallsBackToTheGivenCurrencyWithZeroDays() {
            ChargeSummary summary = StripeMetricsCalculator.summarizeCharges(List.of(), DAY1, DAY2, "EUR");

            assertThat(summary.primaryCurrency()).isEqualTo("eur");
            assertThat(summary.volume()).isEmpty();
            assertThat(summary.dailyRevenue()).hasSize(2)
                    .allSatisfy(d -> assertThat(d.amount()).isEqualByComparingTo("0"));
        }

        @Test
        void recentChargesAreNewestFirstAndIncludeFailures() {
            List<JsonNode> charges = List.of(
                    charge("ch_old", "succeeded", true, 100, 0, "usd", DAY1),
                    charge("ch_new", "failed", false, 200, 0, "usd", DAY3),
                    charge("ch_mid", "succeeded", true, 300, 0, "usd", DAY2));

            ChargeSummary summary = StripeMetricsCalculator.summarizeCharges(charges, DAY1, DAY3, "usd");

            assertThat(summary.recentCharges()).extracting(StripeDtos.RecentCharge::id)
                    .containsExactly("ch_new", "ch_mid", "ch_old");
            assertThat(summary.recentCharges().get(0).status()).isEqualTo("failed");
        }
    }

    @Nested
    class RecurringRevenue {

        @Test
        void normalisesEveryIntervalToAMonth() {
            List<JsonNode> subscriptions = List.of(
                    subscription(item(1000, null, "usd", "month", 1, 2)), // $10 x2 monthly = $20
                    subscription(item(12000, null, "usd", "year", 1, 1)), // $120/yr = $10
                    subscription(item(1000, null, "usd", "week", 1, 1)), // $10/wk = $43.33
                    subscription(item(3000, null, "usd", "month", 3, 1)), // $30 per quarter = $10
                    subscription(item(null, "0.5", "usd", "month", 1, 1000)), // half a cent x1000 = $5
                    subscription(item(900, null, "eur", "month", 1, 1))); // separate currency

            MrrSummary mrr = StripeMetricsCalculator.monthlyRecurringRevenue(subscriptions);

            assertThat(amount(mrr.mrr(), "usd")).isEqualByComparingTo("88.33");
            assertThat(amount(mrr.mrr(), "eur")).isEqualByComparingTo("9.00");
            assertThat(mrr.unpricedItems()).isZero();
        }

        @Test
        void countsUsageBasedItemsInsteadOfGuessing() {
            List<JsonNode> subscriptions = List.of(
                    subscription(item(null, null, "usd", "month", 1, 1)), // metered/tiered
                    subscription(item(1000, null, "usd", "month", 1, 1)));

            MrrSummary mrr = StripeMetricsCalculator.monthlyRecurringRevenue(subscriptions);

            assertThat(amount(mrr.mrr(), "usd")).isEqualByComparingTo("10.00");
            assertThat(mrr.unpricedItems()).isEqualTo(1);
        }

        @Test
        void quantityDefaultsToOneWhenAbsent() {
            ObjectNode item = item(2500, null, "usd", "month", 1, 1);
            item.remove("quantity");

            MrrSummary mrr = StripeMetricsCalculator.monthlyRecurringRevenue(List.of(subscription(item)));

            assertThat(amount(mrr.mrr(), "usd")).isEqualByComparingTo("25.00");
        }

        @Test
        void unknownIntervalIsCountedAsUnpriced() {
            assertThat(StripeMetricsCalculator.monthlyFactor("fortnight", 1)).isNull();
        }
    }

    @Test
    void balanceConvertsEachCurrency() {
        JsonNode entries = MAPPER.createArrayNode()
                .add(MAPPER.createObjectNode().put("amount", 12345).put("currency", "usd"))
                .add(MAPPER.createObjectNode().put("amount", 500).put("currency", "jpy"));

        List<CurrencyAmount> balance = StripeMetricsCalculator.balance(entries);

        assertThat(amount(balance, "usd")).isEqualByComparingTo("123.45");
        assertThat(amount(balance, "jpy")).isEqualByComparingTo("500");
    }

    // --- fixtures -----------------------------------------------------------

    private static ObjectNode charge(String id, String status, boolean captured, long amount, long refunded,
            String currency, LocalDate day) {
        ObjectNode charge = MAPPER.createObjectNode();
        charge.put("id", id);
        charge.put("object", "charge");
        charge.put("status", status);
        charge.put("captured", captured);
        charge.put("amount", amount);
        charge.put("amount_refunded", refunded);
        charge.put("refunded", refunded > 0 && refunded == amount);
        charge.put("currency", currency);
        charge.put("created", day.atTime(12, 0).toEpochSecond(ZoneOffset.UTC));
        charge.putNull("description");
        return charge;
    }

    private static ObjectNode item(Integer unitAmount, String unitAmountDecimal, String currency,
            String interval, int intervalCount, int quantity) {
        ObjectNode price = MAPPER.createObjectNode();
        price.put("currency", currency);
        if (unitAmount == null) {
            price.putNull("unit_amount");
        } else {
            price.put("unit_amount", unitAmount);
        }
        if (unitAmountDecimal == null) {
            price.putNull("unit_amount_decimal");
        } else {
            price.put("unit_amount_decimal", unitAmountDecimal);
        }
        price.putObject("recurring").put("interval", interval).put("interval_count", intervalCount);

        ObjectNode item = MAPPER.createObjectNode();
        item.put("quantity", quantity);
        item.set("price", price);
        return item;
    }

    private static JsonNode subscription(ObjectNode... items) {
        ObjectNode subscription = MAPPER.createObjectNode();
        subscription.put("status", "active");
        var data = subscription.putObject("items").putArray("data");
        for (ObjectNode item : items) {
            data.add(item);
        }
        return subscription;
    }

    private static CurrencyVolume volume(ChargeSummary summary, String currency) {
        return summary.volume().stream().filter(v -> v.currency().equals(currency)).findFirst().orElseThrow();
    }

    private static BigDecimal amount(List<CurrencyAmount> amounts, String currency) {
        return amounts.stream().filter(a -> a.currency().equals(currency)).findFirst().orElseThrow().amount();
    }
}
