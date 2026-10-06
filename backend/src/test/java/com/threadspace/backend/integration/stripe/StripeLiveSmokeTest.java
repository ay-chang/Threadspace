package com.threadspace.backend.integration.stripe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.threadspace.backend.integration.core.IntegrationRepository;
import com.threadspace.backend.integration.core.IntegrationSecretRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * Hits the real api.stripe.com with a deliberately invalid key. Proves requests
 * are well-formed enough to be authenticated, and that Stripe's real error body
 * parses the way the unit tests assume. Opt-in: {@code -Dlive=true}.
 */
@EnabledIfSystemProperty(named = "live", matches = "true")
class StripeLiveSmokeTest {

    private final StripeApiClient client = new StripeApiClient("https://api.stripe.com", new ObjectMapper());

    @Test
    void realStripeRejectsAnInvalidKeyInTheShapeWeParse() {
        assertThatThrownBy(() -> client.get("sk_test_threadspaceSmokeTestInvalidKey00", "/v1/balance", Map.of()))
                .isInstanceOfSatisfying(StripeApiException.class, e -> {
                    System.out.printf("[live] stripe status=%d type=%s code=%s message=%s%n",
                            e.getStatusCode(), e.getType(), e.getCode(), e.getStripeMessage());
                    assertThat(e.getStatusCode()).isEqualTo(401);
                    assertThat(e.getType()).isEqualTo("invalid_request_error");
                    assertThat(e.getStripeMessage()).containsIgnoringCase("invalid api key");
                });
    }

    @Test
    void connectSurfacesTheRejectionAsAUserMessage() {
        StripeIntegrationProvider provider = new StripeIntegrationProvider(
                mock(IntegrationRepository.class), mock(IntegrationSecretRepository.class), client, new ObjectMapper());

        assertThatThrownBy(() -> provider.connect(UUID.randomUUID(), "x",
                Map.of("apiKey", "rk_test_threadspaceSmokeTestInvalidKey00")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Stripe rejected this key");
    }
}
