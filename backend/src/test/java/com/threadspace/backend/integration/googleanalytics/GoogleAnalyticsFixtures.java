package com.threadspace.backend.integration.googleanalytics;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** Response bodies shaped exactly like the GA4 Data API's. */
final class GoogleAnalyticsFixtures {

    private GoogleAnalyticsFixtures() {
    }

    static String gaDate(LocalDate date) {
        return date.format(DateTimeFormatter.BASIC_ISO_DATE);
    }

    /** A batchRunReports response for the five overview reports, with a gap day in the series. */
    static String batchResponse(LocalDate today, String timeZone) {
        return """
                {"kind":"analyticsData#batchRunReports","reports":[
                  {"metricHeaders":[{"name":"activeUsers","type":"TYPE_INTEGER"},{"name":"newUsers","type":"TYPE_INTEGER"},
                     {"name":"sessions","type":"TYPE_INTEGER"},{"name":"screenPageViews","type":"TYPE_INTEGER"},
                     {"name":"engagementRate","type":"TYPE_FLOAT"},{"name":"averageSessionDuration","type":"TYPE_SECONDS"}],
                   "rows":[{"metricValues":[{"value":"1520"},{"value":"980"},{"value":"2104"},{"value":"6311"},
                     {"value":"0.6123"},{"value":"143.7"}]}],
                   "rowCount":1,"metadata":{"currencyCode":"USD","timeZone":"%s"},"kind":"analyticsData#runReport"},
                  {"dimensionHeaders":[{"name":"date"}],
                   "metricHeaders":[{"name":"activeUsers","type":"TYPE_INTEGER"},{"name":"sessions","type":"TYPE_INTEGER"},
                     {"name":"screenPageViews","type":"TYPE_INTEGER"}],
                   "rows":[
                     {"dimensionValues":[{"value":"%s"}],"metricValues":[{"value":"50"},{"value":"70"},{"value":"210"}]},
                     {"dimensionValues":[{"value":"%s"}],"metricValues":[{"value":"61"},{"value":"80"},{"value":"250"}]}],
                   "rowCount":2,"metadata":{"timeZone":"%s"}},
                  {"dimensionHeaders":[{"name":"pagePath"},{"name":"pageTitle"}],
                   "metricHeaders":[{"name":"screenPageViews","type":"TYPE_INTEGER"},{"name":"activeUsers","type":"TYPE_INTEGER"}],
                   "rows":[
                     {"dimensionValues":[{"value":"/"},{"value":"Home"}],"metricValues":[{"value":"3000"},{"value":"900"}]},
                     {"dimensionValues":[{"value":"/pricing"},{"value":"Pricing"}],"metricValues":[{"value":"800"},{"value":"400"}]}]},
                  {"dimensionHeaders":[{"name":"sessionDefaultChannelGroup"}],
                   "metricHeaders":[{"name":"sessions","type":"TYPE_INTEGER"}],
                   "rows":[
                     {"dimensionValues":[{"value":"Organic Search"}],"metricValues":[{"value":"1200"}]},
                     {"dimensionValues":[{"value":"Direct"}],"metricValues":[{"value":"700"}]}]},
                  {"dimensionHeaders":[{"name":"country"}],
                   "metricHeaders":[{"name":"activeUsers","type":"TYPE_INTEGER"}],
                   "rows":[{"dimensionValues":[{"value":"United States"}],"metricValues":[{"value":"800"}]}]}
                ]}""".formatted(timeZone, gaDate(today.minusDays(2)), gaDate(today), timeZone);
    }

    static final String REALTIME_RESPONSE = """
            {"metricHeaders":[{"name":"activeUsers","type":"TYPE_INTEGER"}],
             "rows":[{"metricValues":[{"value":"7"}]}],"rowCount":1,"kind":"analyticsData#runRealtimeReport"}""";

    static final String PERMISSION_DENIED = """
            {"error":{"code":403,"message":"User does not have sufficient permissions for this property.",
             "status":"PERMISSION_DENIED"}}""";

    static final String SERVICE_DISABLED = """
            {"error":{"code":403,
             "message":"Google Analytics Data API has not been used in project 123456 before or it is disabled.",
             "status":"PERMISSION_DENIED","details":[{"@type":"type.googleapis.com/google.rpc.ErrorInfo",
             "reason":"SERVICE_DISABLED","domain":"googleapis.com",
             "metadata":{"consumer":"projects/123456","service":"analyticsdata.googleapis.com"}}]}}""";
}
