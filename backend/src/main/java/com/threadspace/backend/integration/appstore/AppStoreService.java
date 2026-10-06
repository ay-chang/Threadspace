package com.threadspace.backend.integration.appstore;

import java.security.interfaces.ECPrivateKey;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.fasterxml.jackson.databind.JsonNode;
import com.threadspace.backend.integration.appstore.AppStoreDtos.AppInfo;
import com.threadspace.backend.integration.appstore.AppStoreDtos.AppStoreMetrics;
import com.threadspace.backend.integration.appstore.AppStoreSalesReport.Row;
import com.threadspace.backend.integration.core.IntegrationCredentials;
import com.threadspace.backend.integration.core.IntegrationType;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;

@Service
public class AppStoreService {

    /** Apple dates its reports in Pacific time. */
    static final ZoneId REPORT_ZONE = ZoneId.of("America/Los_Angeles");

    static final int DEFAULT_DAYS = 14;
    static final int MAX_DAYS = 30;

    private final IntegrationCredentials credentials;
    private final AppStoreApiClient appStoreApiClient;

    /**
     * Reports are one request per day, so they're fetched concurrently. The pool
     * is small and shared to stay well under Apple's per-key rate limit.
     */
    private final ExecutorService reportFetcher = Executors.newFixedThreadPool(4, runnable -> {
        Thread thread = new Thread(runnable, "appstore-report-fetch");
        thread.setDaemon(true);
        return thread;
    });

    public AppStoreService(IntegrationCredentials credentials, AppStoreApiClient appStoreApiClient) {
        this.credentials = credentials;
        this.appStoreApiClient = appStoreApiClient;
    }

    @PreDestroy
    void shutdown() {
        reportFetcher.shutdownNow();
    }

    public AppStoreMetrics getMetrics(UUID projectId, int days) {
        AppStoreSecretPayload stored = credentials.loadConnected(
                projectId, IntegrationType.APPLE_CONNECT, AppStoreSecretPayload.class, "App Store Connect");
        ECPrivateKey key = AppStoreJwt.parsePrivateKey(stored.privateKey());
        // One token covers every request below; it's valid for 19 minutes.
        String token = AppStoreJwt.create(stored.issuerId(), stored.keyId(), key, Instant.now());

        List<AppInfo> apps = fetchApps(token);

        int window = Math.max(1, Math.min(days, MAX_DAYS));
        // Today's report never exists yet, so the window ends yesterday (Pacific).
        LocalDate end = LocalDate.now(REPORT_ZONE).minusDays(1);
        LocalDate start = end.minusDays(window - 1L);

        Map<LocalDate, CompletableFuture<Optional<List<Row>>>> pending = new LinkedHashMap<>();
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            LocalDate reportDate = date;
            pending.put(reportDate, CompletableFuture.supplyAsync(
                    () -> appStoreApiClient
                            .getDailySalesReport(token, stored.vendorNumber(), reportDate)
                            .map(AppStoreSalesReport::parse),
                    reportFetcher));
        }

        Map<LocalDate, Optional<List<Row>>> reports = new TreeMap<>();
        try {
            pending.forEach((date, future) -> reports.put(date, future.join()));
        } catch (CompletionException e) {
            // Surface the underlying API error so the controller can map it.
            if (e.getCause() instanceof RuntimeException cause) {
                throw cause;
            }
            throw e;
        }

        return AppStoreMetricsCalculator.aggregate(reports, apps);
    }

    private List<AppInfo> fetchApps(String token) {
        JsonNode response = appStoreApiClient.getJson(token, "/v1/apps",
                Map.of("fields[apps]", "name,bundleId", "limit", "200"));
        List<AppInfo> apps = new ArrayList<>();
        for (JsonNode app : response.path("data")) {
            JsonNode attributes = app.path("attributes");
            apps.add(new AppInfo(
                    app.path("id").asText(),
                    attributes.path("name").asText(),
                    attributes.path("bundleId").asText()));
        }
        return apps;
    }
}
