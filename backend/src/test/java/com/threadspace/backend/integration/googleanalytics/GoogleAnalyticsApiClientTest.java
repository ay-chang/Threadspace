package com.threadspace.backend.integration.googleanalytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.threadspace.backend.integration.testsupport.StubServer;
import com.threadspace.backend.integration.testsupport.StubServer.Response;
import org.junit.jupiter.api.Test;

class GoogleAnalyticsApiClientTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void batchPostsEveryRequestToThePropertyWithTheToken() throws Exception {
        try (StubServer ga = new StubServer(req -> Response.json(200, "{\"reports\":[]}"))) {
            GoogleAnalyticsApiClient client = new GoogleAnalyticsApiClient(ga.baseUrl(), mapper);
            List<JsonNode> requests = new GoogleAnalyticsReports(mapper).overviewRequests(7);

            client.batchRunReports("ya29.token", "123456789", requests);

            StubServer.Request request = ga.requests().get(0);
            assertThat(request.method()).isEqualTo("POST");
            assertThat(request.path()).isEqualTo("/v1beta/properties/123456789:batchRunReports");
            assertThat(request.header("Authorization")).isEqualTo("Bearer ya29.token");
            assertThat(request.header("Content-Type")).startsWith("application/json");
            JsonNode body = mapper.readTree(request.body());
            assertThat(body.path("requests")).hasSize(5);
            assertThat(body.path("requests").get(0)).isEqualTo(requests.get(0));
        }
    }

    @Test
    void runReportAndRealtimeUseTheirOwnMethods() throws Exception {
        try (StubServer ga = new StubServer(req -> Response.json(200, "{}"))) {
            GoogleAnalyticsApiClient client = new GoogleAnalyticsApiClient(ga.baseUrl(), mapper);

            client.runReport("t", "42", mapper.createObjectNode());
            client.runRealtimeReport("t", "42", mapper.createObjectNode());

            assertThat(ga.requests()).extracting(StubServer.Request::path).containsExactly(
                    "/v1beta/properties/42:runReport", "/v1beta/properties/42:runRealtimeReport");
        }
    }

    @Test
    void recognisesADisabledApiFromTheErrorInfoReason() throws Exception {
        try (StubServer ga = new StubServer(req -> Response.json(403, GoogleAnalyticsFixtures.SERVICE_DISABLED))) {
            GoogleAnalyticsApiClient client = new GoogleAnalyticsApiClient(ga.baseUrl(), mapper);

            assertThatThrownBy(() -> client.runReport("t", "42", mapper.createObjectNode()))
                    .isInstanceOfSatisfying(GoogleAnalyticsApiException.class, e -> {
                        assertThat(e.getStatusCode()).isEqualTo(403);
                        assertThat(e.getStatus()).isEqualTo("PERMISSION_DENIED");
                        assertThat(e.getReason()).isEqualTo("SERVICE_DISABLED");
                        assertThat(e.isApiDisabled()).isTrue();
                    });
        }
    }

    @Test
    void aMissingPropertyGrantIsNotMistakenForADisabledApi() throws Exception {
        try (StubServer ga = new StubServer(req -> Response.json(403, GoogleAnalyticsFixtures.PERMISSION_DENIED))) {
            GoogleAnalyticsApiClient client = new GoogleAnalyticsApiClient(ga.baseUrl(), mapper);

            assertThatThrownBy(() -> client.runReport("t", "42", mapper.createObjectNode()))
                    .isInstanceOfSatisfying(GoogleAnalyticsApiException.class, e -> {
                        assertThat(e.getReason()).isNull();
                        assertThat(e.isApiDisabled()).isFalse();
                        assertThat(e.getApiMessage()).contains("sufficient permissions");
                    });
        }
    }
}
