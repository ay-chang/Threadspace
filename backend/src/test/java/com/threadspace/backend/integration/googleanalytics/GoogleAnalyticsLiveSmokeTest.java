package com.threadspace.backend.integration.googleanalytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.threadspace.backend.integration.core.IntegrationRepository;
import com.threadspace.backend.integration.core.IntegrationSecretRepository;
import com.threadspace.backend.integration.google.GoogleAccessTokens;
import com.threadspace.backend.integration.google.GoogleServiceAccount;
import com.threadspace.backend.integration.google.GoogleTokenException;
import com.threadspace.backend.integration.testsupport.TestKeys;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * Sends a correctly signed token request for a service account that doesn't
 * exist to Google's real endpoint, and calls the real Data API without a valid
 * token. Opt-in: {@code -Dlive=true}.
 */
@EnabledIfSystemProperty(named = "live", matches = "true")
class GoogleAnalyticsLiveSmokeTest {

    private static final String EMAIL = "threadspace-smoke@threadspace-nonexistent.iam.gserviceaccount.com";

    private final ObjectMapper mapper = new ObjectMapper();
    private final GoogleAccessTokens tokens = new GoogleAccessTokens("https://oauth2.googleapis.com/token");
    private final String keyJson = TestKeys.serviceAccountJson(
            "threadspace-nonexistent", EMAIL, "https://oauth2.googleapis.com/token");

    @Test
    void realGoogleRefusesAnUnknownServiceAccountAsRejected() {
        GoogleServiceAccount account = GoogleServiceAccount.parse(keyJson, mapper);

        assertThatThrownBy(() -> tokens.accessToken(account, GoogleAnalyticsApiClient.SCOPE))
                .isInstanceOfSatisfying(GoogleTokenException.class, e -> {
                    System.out.println("[live] google token: " + e.getMessage().replaceAll("\\s+", " "));
                    assertThat(e.isRejected()).isTrue();
                });
    }

    @Test
    void realDataApiRejectsABogusTokenInTheShapeWeParse() {
        GoogleAnalyticsApiClient client = new GoogleAnalyticsApiClient(
                "https://analyticsdata.googleapis.com", mapper);

        assertThatThrownBy(() -> client.runReport("ya29.not-a-real-token", "123456789",
                new GoogleAnalyticsReports(mapper).probeRequest()))
                .isInstanceOfSatisfying(GoogleAnalyticsApiException.class, e -> {
                    System.out.printf("[live] ga data api status=%d grpc=%s reason=%s message=%s%n",
                            e.getStatusCode(), e.getStatus(), e.getReason(), e.getApiMessage());
                    assertThat(e.getStatusCode()).isEqualTo(401);
                    assertThat(e.getStatus()).isEqualTo("UNAUTHENTICATED");
                });
    }

    @Test
    void connectTurnsTheRejectionIntoAUserMessage() {
        GoogleAnalyticsIntegrationProvider provider = new GoogleAnalyticsIntegrationProvider(
                mock(IntegrationRepository.class), mock(IntegrationSecretRepository.class), tokens,
                new GoogleAnalyticsApiClient("https://analyticsdata.googleapis.com", mapper), mapper);

        assertThatThrownBy(() -> provider.connect(UUID.randomUUID(), "x",
                Map.of("propertyId", "123456789", "serviceAccountJson", keyJson)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Google rejected this service account key");
    }
}
