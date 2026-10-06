package com.threadspace.backend.integration.googleanalytics;

import java.util.List;

/** Response shapes for the Google Analytics metrics endpoint. */
public final class GoogleAnalyticsDtos {

    private GoogleAnalyticsDtos() {
    }

    /**
     * Whole-period totals. {@code engagementRate} is a fraction (0.42 = 42%);
     * {@code averageSessionDuration} is in seconds.
     */
    public record Totals(
            long activeUsers,
            long newUsers,
            long sessions,
            long pageViews,
            double engagementRate,
            double averageSessionDuration) {
    }

    public record DailyTraffic(String date, long activeUsers, long sessions, long pageViews) {
    }

    public record PageViews(String path, String title, long views, long users) {
    }

    public record ChannelSessions(String channel, long sessions) {
    }

    public record CountryUsers(String country, long users) {
    }

    public record GoogleAnalyticsMetrics(
            String propertyId,
            int periodDays,
            String timeZone,
            Totals totals,
            Long activeUsersNow,
            List<DailyTraffic> daily,
            List<PageViews> topPages,
            List<ChannelSessions> channels,
            List<CountryUsers> countries) {
    }
}
