package com.threadspace.backend.integration.stripe;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Set;

/**
 * Converts Stripe amounts, which are integers in the currency's smallest unit,
 * into major units. Most currencies have two decimals, but not all - treating
 * JPY as cents would show a 1000 yen charge as 10 yen.
 *
 * Lists from https://docs.stripe.com/currencies#zero-decimal and #three-decimal.
 */
public final class StripeMoney {

    private static final Set<String> ZERO_DECIMAL = Set.of(
            "bif", "clp", "djf", "gnf", "jpy", "kmf", "krw", "mga",
            "pyg", "rwf", "ugx", "vnd", "vuv", "xaf", "xof", "xpf");

    private static final Set<String> THREE_DECIMAL = Set.of(
            "bhd", "jod", "kwd", "omr", "tnd");

    private StripeMoney() {
    }

    public static int decimals(String currency) {
        String c = currency == null ? "" : currency.toLowerCase(Locale.ROOT);
        if (ZERO_DECIMAL.contains(c)) {
            return 0;
        }
        if (THREE_DECIMAL.contains(c)) {
            return 3;
        }
        return 2;
    }

    public static BigDecimal toMajor(long minorUnits, String currency) {
        return BigDecimal.valueOf(minorUnits).movePointLeft(decimals(currency));
    }

    public static BigDecimal toMajor(BigDecimal minorUnits, String currency) {
        return minorUnits.movePointLeft(decimals(currency));
    }
}
