package com.threadspace.backend.integration.appstore;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.threadspace.backend.integration.appstore.AppStoreDtos.AppInfo;
import com.threadspace.backend.integration.appstore.AppStoreDtos.AppStoreMetrics;
import com.threadspace.backend.integration.appstore.AppStoreDtos.CurrencyProceeds;
import com.threadspace.backend.integration.appstore.AppStoreDtos.DailyUnits;
import com.threadspace.backend.integration.appstore.AppStoreDtos.TitleUnits;
import com.threadspace.backend.integration.appstore.AppStoreSalesReport.Row;
import org.junit.jupiter.api.Test;

class AppStoreMetricsCalculatorTest {

    private static final LocalDate DAY1 = LocalDate.of(2026, 9, 24);
    private static final LocalDate DAY2 = DAY1.plusDays(1);

    @Test
    void aggregatesUnitsByKindAndProceedsPerCurrency() {
        List<Row> day1 = AppStoreSalesReport.parse(
                AppStoreSalesReportTest.sampleReport().getBytes(StandardCharsets.UTF_8));

        Map<LocalDate, Optional<List<Row>>> reports = new LinkedHashMap<>();
        reports.put(DAY1, Optional.of(day1));
        reports.put(DAY2, Optional.empty());

        AppStoreMetrics metrics = AppStoreMetricsCalculator.aggregate(
                reports, List.of(new AppInfo("1111", "Habit Tracker", "com.example.habits")));

        assertThat(metrics.periodDays()).isEqualTo(2);
        assertThat(metrics.totalDownloads()).isEqualTo(12);     // 10 + 2
        assertThat(metrics.totalRedownloads()).isEqualTo(4);
        assertThat(metrics.totalUpdates()).isEqualTo(50);
        assertThat(metrics.totalInAppPurchases()).isEqualTo(2);  // 3 bought, 1 refunded

        // 3 x 2.99 - 1 x 2.99 in USD; 2 x 0.70 in EUR; never added together.
        assertThat(proceeds(metrics, "USD")).isEqualByComparingTo("5.98");
        assertThat(proceeds(metrics, "EUR")).isEqualByComparingTo("1.40");
        assertThat(metrics.proceeds()).hasSize(2);

        assertThat(metrics.apps()).extracting(AppInfo::name).containsExactly("Habit Tracker");
    }

    @Test
    void missingReportsAreZeroDaysNotErrors() {
        Map<LocalDate, Optional<List<Row>>> reports = new LinkedHashMap<>();
        reports.put(DAY1, Optional.of(List.of()));
        reports.put(DAY2, Optional.empty());

        AppStoreMetrics metrics = AppStoreMetricsCalculator.aggregate(reports, List.of());

        assertThat(metrics.daily()).extracting(DailyUnits::date).containsExactly("2026-09-24", "2026-09-25");
        assertThat(metrics.daily()).extracting(DailyUnits::hasReport).containsExactly(true, false);
        assertThat(metrics.daysWithoutReport()).isEqualTo(1);
        assertThat(metrics.latestReportDate()).isEqualTo("2026-09-24");
    }

    @Test
    void latestReportDateIsNullWhenNothingHasBeenPublished() {
        Map<LocalDate, Optional<List<Row>>> reports = new LinkedHashMap<>();
        reports.put(DAY1, Optional.empty());

        assertThat(AppStoreMetricsCalculator.aggregate(reports, List.of()).latestReportDate()).isNull();
    }

    @Test
    void topTitlesRankByDownloadsPlusPurchases() {
        List<Row> day1 = AppStoreSalesReport.parse(
                AppStoreSalesReportTest.sampleReport().getBytes(StandardCharsets.UTF_8));
        Map<LocalDate, Optional<List<Row>>> reports = new LinkedHashMap<>();
        reports.put(DAY1, Optional.of(day1));

        List<TitleUnits> top = AppStoreMetricsCalculator.aggregate(reports, List.of()).topTitles();

        assertThat(top).extracting(TitleUnits::title)
                .containsExactly("Habit Tracker", "Paid Notes", "Pro Upgrade");
        TitleUnits habit = top.get(0);
        assertThat(habit.downloads()).isEqualTo(10);
        assertThat(habit.updates()).isEqualTo(50);
    }

    private static java.math.BigDecimal proceeds(AppStoreMetrics metrics, String currency) {
        return metrics.proceeds().stream()
                .filter(p -> p.currency().equals(currency))
                .map(CurrencyProceeds::amount)
                .findFirst()
                .orElseThrow();
    }
}
