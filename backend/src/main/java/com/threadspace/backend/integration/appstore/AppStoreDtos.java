package com.threadspace.backend.integration.appstore;

import java.math.BigDecimal;
import java.util.List;

/** Response shapes for the App Store Connect metrics endpoint. */
public final class AppStoreDtos {

    private AppStoreDtos() {
    }

    public record AppInfo(String id, String name, String bundleId) {
    }

    /**
     * Unit counts for one report day. {@code hasReport} is false when Apple
     * returned no report - either no sales that day, or not published yet.
     */
    public record DailyUnits(
            String date,
            boolean hasReport,
            int downloads,
            int redownloads,
            int updates,
            int inAppPurchases) {
    }

    public record CurrencyProceeds(String currency, BigDecimal amount) {
    }

    public record TitleUnits(String title, int downloads, int inAppPurchases, int updates) {
    }

    public record AppStoreMetrics(
            int periodDays,
            String latestReportDate,
            List<AppInfo> apps,
            List<DailyUnits> daily,
            int totalDownloads,
            int totalRedownloads,
            int totalUpdates,
            int totalInAppPurchases,
            List<CurrencyProceeds> proceeds,
            List<TitleUnits> topTitles,
            int daysWithoutReport) {
    }
}
