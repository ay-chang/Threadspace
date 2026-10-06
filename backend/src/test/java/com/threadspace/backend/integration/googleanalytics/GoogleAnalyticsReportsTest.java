package com.threadspace.backend.integration.googleanalytics;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.threadspace.backend.integration.googleanalytics.GoogleAnalyticsDtos.DailyTraffic;
import com.threadspace.backend.integration.googleanalytics.GoogleAnalyticsDtos.GoogleAnalyticsMetrics;
import org.junit.jupiter.api.Test;

class GoogleAnalyticsReportsTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 27);

    private final ObjectMapper mapper = new ObjectMapper();
    private final GoogleAnalyticsReports reports = new GoogleAnalyticsReports(mapper);

    @Test
    void overviewRequestsCoverTheWindowEndingToday() {
        List<JsonNode> requests = reports.overviewRequests(28);

        assertThat(requests).hasSize(5); // the batch endpoint's maximum
        assertThat(requests).allSatisfy(r -> {
            assertThat(r.path("dateRanges").get(0).path("startDate").asText()).isEqualTo("27daysAgo");
            assertThat(r.path("dateRanges").get(0).path("endDate").asText()).isEqualTo("today");
        });
        assertThat(requests.get(0).has("dimensions")).isFalse();
        assertThat(requests.get(1).path("dimensions").get(0).path("name").asText()).isEqualTo("date");
        assertThat(requests.get(1).path("orderBys").get(0).path("dimension").path("dimensionName").asText())
                .isEqualTo("date");

        JsonNode pages = requests.get(2);
        assertThat(pages.path("orderBys").get(0).path("metric").path("metricName").asText())
                .isEqualTo("screenPageViews");
        assertThat(pages.path("orderBys").get(0).path("desc").asBoolean()).isTrue();
        assertThat(pages.path("limit").asInt()).isEqualTo(10);
    }

    @Test
    void parsesTotalsAndBreakdowns() throws Exception {
        GoogleAnalyticsMetrics metrics = reports.toMetrics("123456789", 3,
                mapper.readTree(GoogleAnalyticsFixtures.batchResponse(TODAY, "America/Los_Angeles")), 7L, TODAY);

        assertThat(metrics.timeZone()).isEqualTo("America/Los_Angeles");
        assertThat(metrics.totals().activeUsers()).isEqualTo(1520);
        assertThat(metrics.totals().newUsers()).isEqualTo(980);
        assertThat(metrics.totals().sessions()).isEqualTo(2104);
        assertThat(metrics.totals().pageViews()).isEqualTo(6311);
        assertThat(metrics.totals().engagementRate()).isEqualTo(0.6123);
        assertThat(metrics.totals().averageSessionDuration()).isEqualTo(143.7);
        assertThat(metrics.activeUsersNow()).isEqualTo(7);

        assertThat(metrics.topPages()).extracting(p -> p.path() + "|" + p.title() + "|" + p.views() + "|" + p.users())
                .containsExactly("/|Home|3000|900", "/pricing|Pricing|800|400");
        assertThat(metrics.channels()).extracting(c -> c.channel() + "=" + c.sessions())
                .containsExactly("Organic Search=1200", "Direct=700");
        assertThat(metrics.countries().get(0).country()).isEqualTo("United States");
    }

    @Test
    void fillsDaysTheApiOmittedWithZeros() throws Exception {
        GoogleAnalyticsMetrics metrics = reports.toMetrics("123456789", 3,
                mapper.readTree(GoogleAnalyticsFixtures.batchResponse(TODAY, "UTC")), null, TODAY);

        assertThat(metrics.daily()).extracting(DailyTraffic::date)
                .containsExactly("2026-09-25", "2026-09-26", "2026-09-27");
        assertThat(metrics.daily()).extracting(DailyTraffic::activeUsers).containsExactly(50L, 0L, 61L);
        assertThat(metrics.daily()).extracting(DailyTraffic::pageViews).containsExactly(210L, 0L, 250L);
    }

    @Test
    void readsMetricsByHeaderNameNotPosition() throws Exception {
        JsonNode report = mapper.readTree("""
                {"metricHeaders":[{"name":"sessions"},{"name":"activeUsers"}],
                 "rows":[{"metricValues":[{"value":"20"},{"value":"5"}]}]}""");

        var row = GoogleAnalyticsReports.rows(report).get(0);

        assertThat(row).containsEntry("sessions", "20").containsEntry("activeUsers", "5");
    }

    @Test
    void anEmptyPropertyIsAllZerosNotAnError() throws Exception {
        String empty = """
                {"reports":[{"metricHeaders":[{"name":"activeUsers"}],"metadata":{"timeZone":"UTC"}},
                  {"dimensionHeaders":[{"name":"date"}],"metricHeaders":[{"name":"activeUsers"}]},{},{},{}]}""";

        GoogleAnalyticsMetrics metrics = reports.toMetrics("1", 7, mapper.readTree(empty), 0L, TODAY);

        assertThat(metrics.totals().activeUsers()).isZero();
        assertThat(metrics.daily()).hasSize(7).allSatisfy(d -> assertThat(d.activeUsers()).isZero());
        assertThat(metrics.topPages()).isEmpty();
    }

    @Test
    void realtimeCountHandlesNoRows() throws Exception {
        assertThat(GoogleAnalyticsReports.activeUsersNow(mapper.readTree(GoogleAnalyticsFixtures.REALTIME_RESPONSE)))
                .isEqualTo(7);
        assertThat(GoogleAnalyticsReports.activeUsersNow(mapper.readTree("{\"rowCount\":0}"))).isZero();
    }

    @Test
    void todayFollowsThePropertyTimeZoneAndFallsBackToUtc() {
        assertThat(GoogleAnalyticsReports.today("Not/AZone")).isEqualTo(LocalDate.now(ZoneOffset.UTC));
        assertThat(GoogleAnalyticsReports.today("")).isEqualTo(LocalDate.now(ZoneOffset.UTC));
        assertThat(GoogleAnalyticsReports.today("Pacific/Kiritimati"))
                .isEqualTo(LocalDate.now(java.time.ZoneId.of("Pacific/Kiritimati")));
    }
}
