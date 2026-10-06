package com.threadspace.backend.integration.appstore;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import com.threadspace.backend.integration.appstore.AppStoreDtos.AppInfo;
import com.threadspace.backend.integration.appstore.AppStoreDtos.AppStoreMetrics;
import com.threadspace.backend.integration.appstore.AppStoreDtos.CurrencyProceeds;
import com.threadspace.backend.integration.appstore.AppStoreDtos.DailyUnits;
import com.threadspace.backend.integration.appstore.AppStoreDtos.TitleUnits;
import com.threadspace.backend.integration.appstore.AppStoreSalesReport.Row;

/**
 * Pure aggregation over parsed daily reports, kept free of I/O so it can be
 * unit tested. Proceeds are summed per currency, never across currencies.
 */
public final class AppStoreMetricsCalculator {

    private static final int TOP_TITLES = 10;

    private AppStoreMetricsCalculator() {
    }

    /**
     * @param reports one entry per report date, oldest first; an empty Optional
     *                means Apple had no report for that date
     */
    public static AppStoreMetrics aggregate(
            Map<LocalDate, Optional<List<Row>>> reports, List<AppInfo> apps) {

        List<DailyUnits> daily = new ArrayList<>();
        Map<String, BigDecimal> proceeds = new TreeMap<>();
        Map<String, int[]> byTitle = new LinkedHashMap<>(); // downloads, inApp, updates
        int downloads = 0, redownloads = 0, updates = 0, inApp = 0, missing = 0;
        String latestReportDate = null;

        for (Map.Entry<LocalDate, Optional<List<Row>>> entry : reports.entrySet()) {
            String date = entry.getKey().toString();
            if (entry.getValue().isEmpty()) {
                missing++;
                daily.add(new DailyUnits(date, false, 0, 0, 0, 0));
                continue;
            }
            latestReportDate = date;

            int d = 0, r = 0, u = 0, i = 0;
            for (Row row : entry.getValue().get()) {
                switch (row.kind()) {
                    case DOWNLOAD -> d += row.units();
                    case REDOWNLOAD -> r += row.units();
                    case UPDATE -> u += row.units();
                    case IN_APP -> i += row.units();
                    case OTHER -> {
                    }
                }
                if (row.proceedsPerUnit().signum() != 0 && !row.proceedsCurrency().isEmpty()) {
                    proceeds.merge(row.proceedsCurrency(), row.proceeds(), BigDecimal::add);
                }

                int[] t = byTitle.computeIfAbsent(row.title(), k -> new int[3]);
                switch (row.kind()) {
                    case DOWNLOAD -> t[0] += row.units();
                    case IN_APP -> t[1] += row.units();
                    case UPDATE -> t[2] += row.units();
                    default -> {
                    }
                }
            }
            daily.add(new DailyUnits(date, true, d, r, u, i));
            downloads += d;
            redownloads += r;
            updates += u;
            inApp += i;
        }

        List<CurrencyProceeds> proceedsList = new ArrayList<>();
        proceeds.forEach((currency, amount) -> proceedsList.add(
                new CurrencyProceeds(currency, amount.setScale(2, RoundingMode.HALF_UP))));

        List<TitleUnits> topTitles = byTitle.entrySet().stream()
                .map(e -> new TitleUnits(e.getKey(), e.getValue()[0], e.getValue()[1], e.getValue()[2]))
                .filter(t -> t.downloads() + t.inAppPurchases() + t.updates() != 0)
                .sorted(Comparator.comparingInt((TitleUnits t) -> t.downloads() + t.inAppPurchases()).reversed()
                        .thenComparing(TitleUnits::title))
                .limit(TOP_TITLES)
                .toList();

        return new AppStoreMetrics(
                reports.size(),
                latestReportDate,
                apps,
                daily,
                downloads,
                redownloads,
                updates,
                inApp,
                proceedsList,
                topTitles,
                missing);
    }
}
