package com.threadspace.backend.integration.appstore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.threadspace.backend.integration.testsupport.StubServer;
import com.threadspace.backend.integration.testsupport.StubServer.Response;
import com.threadspace.backend.integration.testsupport.TestKeys;
import org.junit.jupiter.api.Test;

class AppStoreApiClientTest {

    private static final String NOT_AUTHORIZED = "{\"errors\":[{\"status\":\"401\",\"code\":\"NOT_AUTHORIZED\","
            + "\"title\":\"Authentication credentials are missing or invalid.\","
            + "\"detail\":\"Provide a properly configured and signed bearer token.\"}]}";

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void jsonRequestsCarryTheBearerTokenAndEncodedFilters() throws Exception {
        try (StubServer server = new StubServer(req -> Response.json(200,
                "{\"data\":[{\"id\":\"1111\",\"attributes\":{\"name\":\"Habit Tracker\"}}]}"))) {
            AppStoreApiClient client = new AppStoreApiClient(server.baseUrl(), mapper);

            JsonNode body = client.getJson("jwt.token.here", "/v1/apps", Map.of("fields[apps]", "name,bundleId"));

            assertThat(body.path("data").get(0).path("id").asText()).isEqualTo("1111");
            StubServer.Request request = server.requests().get(0);
            assertThat(request.path()).isEqualTo("/v1/apps");
            assertThat(request.header("Authorization")).isEqualTo("Bearer jwt.token.here");
            assertThat(request.query()).containsEntry("fields[apps]", "name,bundleId");
        }
    }

    @Test
    void salesReportRequestSendsEveryRequiredFilter() throws Exception {
        byte[] gz = TestKeys.gzip("Title\tUnits\n".getBytes(StandardCharsets.UTF_8));
        try (StubServer server = new StubServer(req -> Response.bytes(200, "application/a-gzip", gz))) {
            AppStoreApiClient client = new AppStoreApiClient(server.baseUrl(), mapper);

            Optional<byte[]> report = client.getDailySalesReport("tok", "85012345", LocalDate.of(2026, 9, 25));

            assertThat(report).hasValueSatisfying(bytes -> assertThat(bytes).isEqualTo(gz));
            StubServer.Request request = server.requests().get(0);
            assertThat(request.path()).isEqualTo("/v1/salesReports");
            assertThat(request.query())
                    .containsEntry("filter[frequency]", "DAILY")
                    .containsEntry("filter[reportDate]", "2026-09-25")
                    .containsEntry("filter[reportSubType]", "SUMMARY")
                    .containsEntry("filter[reportType]", "SALES")
                    .containsEntry("filter[vendorNumber]", "85012345")
                    .containsEntry("filter[version]", AppStoreApiClient.SALES_REPORT_VERSION);
            assertThat(request.header("Accept")).contains("application/a-gzip");
        }
    }

    @Test
    void aMissingReportIsEmptyNotAnError() throws Exception {
        String noSales = "{\"errors\":[{\"status\":\"404\",\"code\":\"NOT_FOUND\","
                + "\"detail\":\"There were no sales for the date specified.\"}]}";
        try (StubServer server = new StubServer(req -> Response.json(404, noSales))) {
            AppStoreApiClient client = new AppStoreApiClient(server.baseUrl(), mapper);

            assertThat(client.getDailySalesReport("tok", "85012345", LocalDate.of(2026, 9, 25))).isEmpty();
        }
    }

    @Test
    void appleErrorsAreParsed() throws Exception {
        try (StubServer server = new StubServer(req -> Response.json(401, NOT_AUTHORIZED))) {
            AppStoreApiClient client = new AppStoreApiClient(server.baseUrl(), mapper);

            assertThatThrownBy(() -> client.getJson("bad", "/v1/apps", Map.of()))
                    .isInstanceOfSatisfying(AppStoreApiException.class, e -> {
                        assertThat(e.getStatusCode()).isEqualTo(401);
                        assertThat(e.getCode()).isEqualTo("NOT_AUTHORIZED");
                        assertThat(e.getDetail()).contains("signed bearer token");
                    });
        }
    }

    @Test
    void aForbiddenSalesReportIsAnErrorNotAnEmptyDay() throws Exception {
        String forbidden = "{\"errors\":[{\"status\":\"403\",\"code\":\"FORBIDDEN_ERROR\","
                + "\"detail\":\"This request is forbidden for security reasons\"}]}";
        try (StubServer server = new StubServer(req -> Response.json(403, forbidden))) {
            AppStoreApiClient client = new AppStoreApiClient(server.baseUrl(), mapper);

            assertThatThrownBy(() -> client.getDailySalesReport("tok", "85012345", LocalDate.of(2026, 9, 25)))
                    .isInstanceOfSatisfying(AppStoreApiException.class,
                            e -> assertThat(e.getStatusCode()).isEqualTo(403));
        }
    }
}
