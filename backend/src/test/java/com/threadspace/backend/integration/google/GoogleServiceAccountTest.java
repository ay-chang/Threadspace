package com.threadspace.backend.integration.google;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.ServerSocket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.threadspace.backend.integration.testsupport.StubServer;
import com.threadspace.backend.integration.testsupport.StubServer.Response;
import com.threadspace.backend.integration.testsupport.TestKeys;
import org.junit.jupiter.api.Test;

class GoogleServiceAccountTest {

    private static final String EMAIL = "threadspace@my-project.iam.gserviceaccount.com";
    private static final String SCOPE = "https://www.googleapis.com/auth/analytics.readonly";

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void parsesAValidKeyFile() {
        String json = TestKeys.serviceAccountJson("my-project", EMAIL, "https://oauth2.googleapis.com/token");

        GoogleServiceAccount account = GoogleServiceAccount.parse("  " + json + "\n", mapper);

        assertThat(account.projectId()).isEqualTo("my-project");
        assertThat(account.clientEmail()).isEqualTo(EMAIL);
        assertThat(account.json()).isEqualTo(json);
    }

    @Test
    void namesEachKindOfBadPaste() {
        assertThatThrownBy(() -> GoogleServiceAccount.parse("", mapper)).hasMessageContaining("required");
        assertThatThrownBy(() -> GoogleServiceAccount.parse("{\"type\":", mapper))
                .hasMessageContaining("not valid JSON");
        assertThatThrownBy(() -> GoogleServiceAccount.parse("[1,2]", mapper))
                .hasMessageContaining("must be a JSON object");
        assertThatThrownBy(() -> GoogleServiceAccount.parse("{\"type\":\"authorized_user\"}", mapper))
                .hasMessageContaining("got \"authorized_user\"");

        String withoutKeyId = TestKeys.serviceAccountJson("p", EMAIL, "https://x")
                .replace("\"private_key_id\": \"0123456789abcdef0123456789abcdef01234567\",", "");
        assertThatThrownBy(() -> GoogleServiceAccount.parse(withoutKeyId, mapper))
                .hasMessageContaining("missing required field \"private_key_id\"");
    }

    @Test
    void tokenExchangeUsesTheConfiguredEndpointNotTheOneInTheFile() throws Exception {
        try (StubServer google = new StubServer(req -> Response.json(200,
                "{\"access_token\":\"ya29.test-token\",\"expires_in\":3599,\"token_type\":\"Bearer\"}"))) {
            // The file points somewhere else entirely; that must never be contacted.
            String json = TestKeys.serviceAccountJson("my-project", EMAIL, "https://attacker.invalid/token");
            GoogleAccessTokens tokens = new GoogleAccessTokens(google.baseUrl() + "/token");

            String token = tokens.accessToken(GoogleServiceAccount.parse(json, mapper), SCOPE);

            assertThat(token).isEqualTo("ya29.test-token");
            StubServer.Request request = google.requests().get(0);
            assertThat(request.method()).isEqualTo("POST");
            assertThat(request.path()).isEqualTo("/token");

            // The body is the OAuth JWT-bearer grant, with a correctly addressed assertion.
            Map<String, String> form = parseForm(request.body());
            assertThat(form).containsEntry("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer");
            JsonNode claims = mapper.readTree(Base64.getUrlDecoder().decode(form.get("assertion").split("\\.")[1]));
            assertThat(claims.path("iss").asText()).isEqualTo(EMAIL);
            assertThat(claims.path("scope").asText()).isEqualTo(SCOPE);
            // The auth library always addresses the assertion to Google's own token
            // URL; the file's token_uri must not leak into it either.
            assertThat(claims.path("aud").asText()).isEqualTo("https://oauth2.googleapis.com/token");
            assertThat(request.body()).doesNotContain("attacker.invalid");
        }
    }

    @Test
    void aRefusedKeyIsReportedAsRejected() throws Exception {
        try (StubServer google = new StubServer(req -> Response.json(400,
                "{\"error\":\"invalid_grant\",\"error_description\":\"Invalid JWT Signature.\"}"))) {
            GoogleAccessTokens tokens = new GoogleAccessTokens(google.baseUrl() + "/token");
            GoogleServiceAccount account = GoogleServiceAccount.parse(
                    TestKeys.serviceAccountJson("p", EMAIL, "https://x"), mapper);

            assertThatThrownBy(() -> tokens.accessToken(account, SCOPE))
                    .isInstanceOfSatisfying(GoogleTokenException.class, e -> assertThat(e.isRejected()).isTrue());
        }
    }

    @Test
    void anUnreachableEndpointIsNotReportedAsRejected() throws Exception {
        int closedPort;
        try (ServerSocket socket = new ServerSocket(0)) {
            closedPort = socket.getLocalPort();
        }
        GoogleAccessTokens tokens = new GoogleAccessTokens("http://127.0.0.1:" + closedPort + "/token");
        GoogleServiceAccount account = GoogleServiceAccount.parse(
                TestKeys.serviceAccountJson("p", EMAIL, "https://x"), mapper);

        assertThatThrownBy(() -> tokens.accessToken(account, SCOPE))
                .isInstanceOfSatisfying(GoogleTokenException.class, e -> assertThat(e.isRejected()).isFalse());
    }

    private static Map<String, String> parseForm(String body) {
        return java.util.Arrays.stream(body.split("&"))
                .map(p -> p.split("=", 2))
                .collect(Collectors.toMap(
                        kv -> URLDecoder.decode(kv[0], StandardCharsets.UTF_8),
                        kv -> URLDecoder.decode(kv[1], StandardCharsets.UTF_8)));
    }
}
