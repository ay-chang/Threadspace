package com.threadspace.backend.integration.stripe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.threadspace.backend.integration.testsupport.StubServer;
import com.threadspace.backend.integration.testsupport.StubServer.Response;
import org.junit.jupiter.api.Test;

class StripeApiClientTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void sendsBearerKeyPinnedVersionAndBracketedParams() throws Exception {
        try (StubServer server = new StubServer(req -> Response.json(200, "{\"object\":\"balance\"}"))) {
            StripeApiClient client = new StripeApiClient(server.baseUrl(), mapper);

            client.get("rk_test_abc", "/v1/charges", Map.of("created[gte]", "1700000000"));

            StubServer.Request request = server.requests().get(0);
            assertThat(request.method()).isEqualTo("GET");
            assertThat(request.path()).isEqualTo("/v1/charges");
            assertThat(request.header("Authorization")).isEqualTo("Bearer rk_test_abc");
            assertThat(request.header("Stripe-Version")).isEqualTo(StripeApiClient.STRIPE_VERSION);
            // The bracketed key must survive encoding and decode back intact.
            assertThat(request.query()).containsEntry("created[gte]", "1700000000");
            assertThat(request.rawQuery()).contains("created%5Bgte%5D=1700000000");
        }
    }

    @Test
    void listFollowsCursorPaginationUntilHasMoreIsFalse() throws Exception {
        AtomicInteger page = new AtomicInteger();
        try (StubServer server = new StubServer(req -> switch (page.getAndIncrement()) {
            case 0 -> Response.json(200, "{\"has_more\":true,\"data\":[{\"id\":\"ch_1\"},{\"id\":\"ch_2\"}]}");
            case 1 -> Response.json(200, "{\"has_more\":true,\"data\":[{\"id\":\"ch_3\"}]}");
            default -> Response.json(200, "{\"has_more\":false,\"data\":[{\"id\":\"ch_4\"}]}");
        })) {
            StripeApiClient client = new StripeApiClient(server.baseUrl(), mapper);

            StripeApiClient.ListResult result = client.list("sk_test_x", "/v1/charges", Map.of(), 1000);

            assertThat(result.items()).extracting(n -> n.path("id").asText())
                    .containsExactly("ch_1", "ch_2", "ch_3", "ch_4");
            assertThat(result.truncated()).isFalse();
            assertThat(server.requests()).hasSize(3);
            assertThat(server.requests().get(0).query()).doesNotContainKey("starting_after");
            assertThat(server.requests().get(1).query()).containsEntry("starting_after", "ch_2");
            assertThat(server.requests().get(2).query()).containsEntry("starting_after", "ch_3");
            assertThat(server.requests()).allSatisfy(r -> assertThat(r.query()).containsEntry("limit", "100"));
        }
    }

    @Test
    void listStopsAtTheCapAndReportsTruncation() throws Exception {
        AtomicInteger counter = new AtomicInteger();
        try (StubServer server = new StubServer(req -> {
            int limit = Integer.parseInt(req.query().get("limit"));
            StringBuilder data = new StringBuilder();
            for (int i = 0; i < limit; i++) {
                if (i > 0) {
                    data.append(',');
                }
                data.append("{\"id\":\"ch_").append(counter.incrementAndGet()).append("\"}");
            }
            return Response.json(200, "{\"has_more\":true,\"data\":[" + data + "]}");
        })) {
            StripeApiClient client = new StripeApiClient(server.baseUrl(), mapper);

            StripeApiClient.ListResult result = client.list("sk_test_x", "/v1/charges", Map.of(), 150);

            assertThat(result.items()).hasSize(150);
            assertThat(result.truncated()).isTrue();
            // The second page asks only for what's left under the cap.
            assertThat(server.requests()).extracting(r -> r.query().get("limit")).containsExactly("100", "50");
        }
    }

    @Test
    void parsesStripeErrorBodies() throws Exception {
        String body = "{\"error\":{\"type\":\"invalid_request_error\",\"code\":\"api_key_invalid\","
                + "\"message\":\"Invalid API Key provided: sk_test_****abcd\"}}";
        try (StubServer server = new StubServer(req -> Response.json(401, body))) {
            StripeApiClient client = new StripeApiClient(server.baseUrl(), mapper);

            assertThatThrownBy(() -> client.get("sk_test_bad", "/v1/balance", Map.of()))
                    .isInstanceOfSatisfying(StripeApiException.class, e -> {
                        assertThat(e.getStatusCode()).isEqualTo(401);
                        assertThat(e.getType()).isEqualTo("invalid_request_error");
                        assertThat(e.getCode()).isEqualTo("api_key_invalid");
                        assertThat(e.getStripeMessage()).startsWith("Invalid API Key provided");
                    });
        }
    }

    @Test
    void keepsTheStatusWhenTheErrorBodyIsNotStripeJson() throws Exception {
        try (StubServer server = new StubServer(
                req -> Response.bytes(502, "text/html", "<html>Bad gateway</html>".getBytes()))) {
            StripeApiClient client = new StripeApiClient(server.baseUrl(), mapper);

            assertThatThrownBy(() -> client.get("sk_test_x", "/v1/balance", Map.of()))
                    .isInstanceOfSatisfying(StripeApiException.class, e -> {
                        assertThat(e.getStatusCode()).isEqualTo(502);
                        assertThat(e.getStripeMessage()).isNull();
                    });
        }
    }
}
