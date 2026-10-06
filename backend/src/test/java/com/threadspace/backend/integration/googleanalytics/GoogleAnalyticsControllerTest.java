package com.threadspace.backend.integration.googleanalytics;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import com.threadspace.backend.integration.google.GoogleTokenException;
import com.threadspace.backend.integration.googleanalytics.GoogleAnalyticsDtos.ChannelSessions;
import com.threadspace.backend.integration.googleanalytics.GoogleAnalyticsDtos.CountryUsers;
import com.threadspace.backend.integration.googleanalytics.GoogleAnalyticsDtos.DailyTraffic;
import com.threadspace.backend.integration.googleanalytics.GoogleAnalyticsDtos.GoogleAnalyticsMetrics;
import com.threadspace.backend.integration.googleanalytics.GoogleAnalyticsDtos.PageViews;
import com.threadspace.backend.integration.googleanalytics.GoogleAnalyticsDtos.Totals;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Pins the JSON contract the traffic page reads, and the error-to-message mapping. */
@WebMvcTest(GoogleAnalyticsController.class)
@TestPropertySource(properties = "INTERNAL_SYNC_TOKEN=test-token")
class GoogleAnalyticsControllerTest {

    private static final UUID PROJECT = UUID.randomUUID();

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private GoogleAnalyticsService service;

    @Test
    void rejectsRequestsWithoutTheInternalToken() throws Exception {
        mvc.perform(get("/projects/{id}/google-analytics/metrics", PROJECT)).andExpect(status().isUnauthorized());
    }

    @Test
    void servesTheFieldsTheTrafficPageReads() throws Exception {
        when(service.getMetrics(eq(PROJECT), anyInt())).thenReturn(new GoogleAnalyticsMetrics(
                "123456789", 28, "America/New_York",
                new Totals(1520, 980, 2104, 6311, 0.6123, 143.7),
                7L,
                List.of(new DailyTraffic("2026-09-27", 61, 80, 250)),
                List.of(new PageViews("/", "Home", 3000, 900)),
                List.of(new ChannelSessions("Organic Search", 1200)),
                List.of(new CountryUsers("United States", 800))));

        mvc.perform(get("/projects/{id}/google-analytics/metrics", PROJECT)
                .param("days", "28")
                .header("x-internal-token", "test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.propertyId").value("123456789"))
                .andExpect(jsonPath("$.periodDays").value(28))
                .andExpect(jsonPath("$.timeZone").value("America/New_York"))
                .andExpect(jsonPath("$.totals.activeUsers").value(1520))
                .andExpect(jsonPath("$.totals.newUsers").value(980))
                .andExpect(jsonPath("$.totals.sessions").value(2104))
                .andExpect(jsonPath("$.totals.pageViews").value(6311))
                .andExpect(jsonPath("$.totals.engagementRate").value(Matchers.closeTo(0.6123, 0.0001)))
                .andExpect(jsonPath("$.totals.averageSessionDuration").value(Matchers.closeTo(143.7, 0.001)))
                .andExpect(jsonPath("$.activeUsersNow").value(7))
                .andExpect(jsonPath("$.daily[0].date").value("2026-09-27"))
                .andExpect(jsonPath("$.daily[0].pageViews").value(250))
                .andExpect(jsonPath("$.topPages[0].path").value("/"))
                .andExpect(jsonPath("$.topPages[0].title").value("Home"))
                .andExpect(jsonPath("$.topPages[0].views").value(3000))
                .andExpect(jsonPath("$.topPages[0].users").value(900))
                .andExpect(jsonPath("$.channels[0].channel").value("Organic Search"))
                .andExpect(jsonPath("$.countries[0].users").value(800));

        verify(service).getMetrics(PROJECT, 28);
    }

    @Test
    void aRevokedPropertyGrantGetsAnActionableMessage() throws Exception {
        when(service.getMetrics(any(), anyInt()))
                .thenThrow(new GoogleAnalyticsApiException(403, "PERMISSION_DENIED", null, "no permission", null));

        mvc.perform(get("/projects/{id}/google-analytics/metrics", PROJECT).header("x-internal-token", "test-token"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(Matchers.containsString("Property access management")));
    }

    @Test
    void aDeletedKeyGetsAnActionableMessage() throws Exception {
        when(service.getMetrics(any(), anyInt())).thenThrow(new GoogleTokenException(true, "invalid_grant", null));

        mvc.perform(get("/projects/{id}/google-analytics/metrics", PROJECT).header("x-internal-token", "test-token"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(Matchers.containsString("reconnect with a new key")));
    }

    @Test
    void anUnreachableGoogleIsATemporaryGatewayError() throws Exception {
        when(service.getMetrics(any(), anyInt())).thenThrow(new GoogleTokenException(false, "timeout", null));

        mvc.perform(get("/projects/{id}/google-analytics/metrics", PROJECT).header("x-internal-token", "test-token"))
                .andExpect(status().isBadGateway());
    }
}
