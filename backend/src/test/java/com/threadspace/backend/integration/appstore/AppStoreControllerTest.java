package com.threadspace.backend.integration.appstore;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.threadspace.backend.integration.appstore.AppStoreDtos.AppInfo;
import com.threadspace.backend.integration.appstore.AppStoreDtos.AppStoreMetrics;
import com.threadspace.backend.integration.appstore.AppStoreDtos.CurrencyProceeds;
import com.threadspace.backend.integration.appstore.AppStoreDtos.DailyUnits;
import com.threadspace.backend.integration.appstore.AppStoreDtos.TitleUnits;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Pins the JSON contract the sales page reads, and the error-to-message mapping. */
@WebMvcTest(AppStoreController.class)
@TestPropertySource(properties = "INTERNAL_SYNC_TOKEN=test-token")
class AppStoreControllerTest {

    private static final UUID PROJECT = UUID.randomUUID();

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AppStoreService service;

    @Test
    void rejectsRequestsWithoutTheInternalToken() throws Exception {
        mvc.perform(get("/projects/{id}/appstore/metrics", PROJECT)).andExpect(status().isUnauthorized());
    }

    @Test
    void servesTheFieldsTheSalesPageReads() throws Exception {
        when(service.getMetrics(eq(PROJECT), anyInt())).thenReturn(new AppStoreMetrics(
                14, "2026-09-25",
                List.of(new AppInfo("1111", "Habit Tracker", "com.example.habits")),
                List.of(new DailyUnits("2026-09-25", true, 10, 4, 50, 2),
                        new DailyUnits("2026-09-26", false, 0, 0, 0, 0)),
                12, 4, 50, 2,
                List.of(new CurrencyProceeds("USD", new BigDecimal("5.98"))),
                List.of(new TitleUnits("Habit Tracker", 10, 0, 50)),
                1));

        mvc.perform(get("/projects/{id}/appstore/metrics", PROJECT)
                .param("days", "14")
                .header("x-internal-token", "test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.periodDays").value(14))
                .andExpect(jsonPath("$.latestReportDate").value("2026-09-25"))
                .andExpect(jsonPath("$.apps[0].bundleId").value("com.example.habits"))
                .andExpect(jsonPath("$.daily[1].hasReport").value(false))
                .andExpect(jsonPath("$.daily[0].inAppPurchases").value(2))
                .andExpect(jsonPath("$.totalDownloads").value(12))
                .andExpect(jsonPath("$.totalRedownloads").value(4))
                .andExpect(jsonPath("$.totalUpdates").value(50))
                .andExpect(jsonPath("$.totalInAppPurchases").value(2))
                .andExpect(jsonPath("$.proceeds[0].amount").value(Matchers.closeTo(5.98, 0.001)))
                .andExpect(jsonPath("$.topTitles[0].title").value("Habit Tracker"))
                .andExpect(jsonPath("$.daysWithoutReport").value(1));

        verify(service).getMetrics(PROJECT, 14);
    }

    @Test
    void aKeyWithoutTheSalesRoleGetsAnActionableMessage() throws Exception {
        when(service.getMetrics(any(), anyInt()))
                .thenThrow(new AppStoreApiException(403, "FORBIDDEN_ERROR", "forbidden", null));

        mvc.perform(get("/projects/{id}/appstore/metrics", PROJECT).header("x-internal-token", "test-token"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(Matchers.containsString("Sales, Finance, or Admin")));
    }

    @Test
    void aRevokedKeyGetsAnActionableMessage() throws Exception {
        when(service.getMetrics(any(), anyInt()))
                .thenThrow(new AppStoreApiException(401, "NOT_AUTHORIZED", "expired", null));

        mvc.perform(get("/projects/{id}/appstore/metrics", PROJECT).header("x-internal-token", "test-token"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(Matchers.containsString("reconnect with a new key")));
    }
}
