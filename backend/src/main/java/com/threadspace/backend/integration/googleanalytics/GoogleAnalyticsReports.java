package com.threadspace.backend.integration.googleanalytics;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.threadspace.backend.integration.googleanalytics.GoogleAnalyticsDtos.ChannelSessions;
import com.threadspace.backend.integration.googleanalytics.GoogleAnalyticsDtos.CountryUsers;
import com.threadspace.backend.integration.googleanalytics.GoogleAnalyticsDtos.DailyTraffic;
import com.threadspace.backend.integration.googleanalytics.GoogleAnalyticsDtos.GoogleAnalyticsMetrics;
import com.threadspace.backend.integration.googleanalytics.GoogleAnalyticsDtos.PageViews;
import com.threadspace.backend.integration.googleanalytics.GoogleAnalyticsDtos.Totals;

/**
 * Builds the Data API report requests and turns the responses into metrics.
 * Pure - no I/O - so both halves can be unit tested.
 *
 * The API returns every metric as a string inside positional arrays; values are
 * looked up by header name so a reordered request can't silently shuffle them.
 */
public final class GoogleAnalyticsReports {

    static final int TOP_ROWS = 10;
    private static final DateTimeFormatter GA_DATE = DateTimeFormatter.BASIC_ISO_DATE; // yyyyMMdd

    private final ObjectMapper mapper;

    public GoogleAnalyticsReports(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    /** The five reports the metrics page shows, in the order {@link #toMetrics} expects them back. */
    public List<JsonNode> overviewRequests(int days) {
        return List.of(
                request(days, List.of(), List.of("activeUsers", "newUsers", "sessions", "screenPageViews",
                        "engagementRate", "averageSessionDuration"), null, 0),
                request(days, List.of("date"), List.of("activeUsers", "sessions", "screenPageViews"),
                        "date", 0),
                request(days, List.of("pagePath", "pageTitle"), List.of("screenPageViews", "activeUsers"),
                        "screenPageViews", TOP_ROWS),
                request(days, List.of("sessionDefaultChannelGroup"), List.of("sessions"), "sessions", TOP_ROWS),
                request(days, List.of("country"), List.of("activeUsers"), "activeUsers", TOP_ROWS));
    }

    /** The cheapest possible report: proves the account can read the property. */
    public JsonNode probeRequest() {
        ObjectNode request = request(2, List.of(), List.of("activeUsers"), null, 0);
        request.put("limit", 1);
        return request;
    }

    public JsonNode realtimeRequest() {
        ObjectNode request = mapper.createObjectNode();
        request.putArray("metrics").addObject().put("name", "activeUsers");
        return request;
    }

    public GoogleAnalyticsMetrics toMetrics(String propertyId, int days, JsonNode batchResponse,
            Long activeUsersNow, LocalDate today) {
        JsonNode reports = batchResponse.path("reports");
        JsonNode totalsReport = reports.path(0);
        String timeZone = totalsReport.path("metadata").path("timeZone").asText("");

        Map<String, String> totalsRow = rows(totalsReport).stream().findFirst().orElse(Map.of());
        Totals totals = new Totals(
                asLong(totalsRow.get("activeUsers")),
                asLong(totalsRow.get("newUsers")),
                asLong(totalsRow.get("sessions")),
                asLong(totalsRow.get("screenPageViews")),
                asDouble(totalsRow.get("engagementRate")),
                asDouble(totalsRow.get("averageSessionDuration")));

        return new GoogleAnalyticsMetrics(
                propertyId,
                days,
                timeZone,
                totals,
                activeUsersNow,
                daily(reports.path(1), days, today),
                rows(reports.path(2)).stream()
                        .map(r -> new PageViews(r.get("pagePath"), r.get("pageTitle"),
                                asLong(r.get("screenPageViews")), asLong(r.get("activeUsers"))))
                        .toList(),
                rows(reports.path(3)).stream()
                        .map(r -> new ChannelSessions(r.get("sessionDefaultChannelGroup"), asLong(r.get("sessions"))))
                        .toList(),
                rows(reports.path(4)).stream()
                        .map(r -> new CountryUsers(r.get("country"), asLong(r.get("activeUsers"))))
                        .toList());
    }

    /** "Today" as the property sees it; falls back to UTC for a missing or unknown zone. */
    public static LocalDate today(String timeZone) {
        try {
            return timeZone == null || timeZone.isBlank()
                    ? LocalDate.now(ZoneOffset.UTC)
                    : LocalDate.now(ZoneId.of(timeZone));
        } catch (DateTimeException e) {
            return LocalDate.now(ZoneOffset.UTC);
        }
    }

    public static Long activeUsersNow(JsonNode realtimeResponse) {
        JsonNode value = realtimeResponse.path("rows").path(0).path("metricValues").path(0).path("value");
        return value.isMissingNode() ? 0L : asLong(value.asText());
    }

    /**
     * The API omits days with no traffic, so the series is rebuilt day by day
     * over the window and gaps are filled with zeros.
     */
    private List<DailyTraffic> daily(JsonNode report, int days, LocalDate today) {
        Map<LocalDate, Map<String, String>> byDate = new HashMap<>();
        for (Map<String, String> row : rows(report)) {
            try {
                byDate.put(LocalDate.parse(row.get("date"), GA_DATE), row);
            } catch (Exception ignored) {
                // Skip rows with an unparseable date rather than failing the page.
            }
        }

        List<DailyTraffic> series = new ArrayList<>();
        for (LocalDate d = today.minusDays(days - 1L); !d.isAfter(today); d = d.plusDays(1)) {
            Map<String, String> row = byDate.getOrDefault(d, Map.of());
            series.add(new DailyTraffic(d.toString(), asLong(row.get("activeUsers")),
                    asLong(row.get("sessions")), asLong(row.get("screenPageViews"))));
        }
        return series;
    }

    /** Flattens a report into name->value maps using its dimension and metric headers. */
    static List<Map<String, String>> rows(JsonNode report) {
        List<String> dimensions = new ArrayList<>();
        report.path("dimensionHeaders").forEach(h -> dimensions.add(h.path("name").asText()));
        List<String> metrics = new ArrayList<>();
        report.path("metricHeaders").forEach(h -> metrics.add(h.path("name").asText()));

        List<Map<String, String>> result = new ArrayList<>();
        for (JsonNode row : report.path("rows")) {
            Map<String, String> values = new LinkedHashMap<>();
            for (int i = 0; i < dimensions.size(); i++) {
                values.put(dimensions.get(i), row.path("dimensionValues").path(i).path("value").asText(""));
            }
            for (int i = 0; i < metrics.size(); i++) {
                values.put(metrics.get(i), row.path("metricValues").path(i).path("value").asText(""));
            }
            result.add(values);
        }
        return result;
    }

    private ObjectNode request(int days, List<String> dimensions, List<String> metrics, String orderBy, int limit) {
        ObjectNode request = mapper.createObjectNode();
        request.putArray("dateRanges").addObject()
                .put("startDate", (days - 1) + "daysAgo")
                .put("endDate", "today");
        ArrayNode dims = request.putArray("dimensions");
        dimensions.forEach(d -> dims.addObject().put("name", d));
        if (dimensions.isEmpty()) {
            request.remove("dimensions");
        }
        ArrayNode mets = request.putArray("metrics");
        metrics.forEach(m -> mets.addObject().put("name", m));

        if (orderBy != null) {
            ObjectNode order = request.putArray("orderBys").addObject();
            if (dimensions.contains(orderBy)) {
                order.putObject("dimension").put("dimensionName", orderBy);
            } else {
                order.putObject("metric").put("metricName", orderBy);
                order.put("desc", true);
            }
        }
        if (limit > 0) {
            request.put("limit", limit);
        }
        return request;
    }

    private static long asLong(String value) {
        if (value == null || value.isBlank()) {
            return 0L;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            try {
                return Math.round(Double.parseDouble(value));
            } catch (NumberFormatException ignored) {
                return 0L;
            }
        }
    }

    private static double asDouble(String value) {
        if (value == null || value.isBlank()) {
            return 0d;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return 0d;
        }
    }
}
