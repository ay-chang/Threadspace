package com.threadspace.backend.integration.appstore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.threadspace.backend.integration.core.IntegrationRepository;
import com.threadspace.backend.integration.core.IntegrationSecretRepository;
import com.threadspace.backend.integration.testsupport.TestKeys;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * Hits the real App Store Connect API with a correctly-formed token signed by a
 * key Apple has never seen. Proves the request reaches Apple and its real error
 * body parses the way the unit tests assume. Opt-in: {@code -Dlive=true}.
 */
@EnabledIfSystemProperty(named = "live", matches = "true")
class AppStoreLiveSmokeTest {

    private static final String ISSUER = "57246542-96fe-1a63-e053-0824d011072a";
    private static final String KEY_ID = "2X9R4HXF34";

    private final AppStoreApiClient client = new AppStoreApiClient(
            "https://api.appstoreconnect.apple.com", new ObjectMapper());
    private final String pem = TestKeys.pkcs8Pem(TestKeys.ecKeyPair("secp256r1"));

    @Test
    void realAppleRejectsAnUnknownKeyInTheShapeWeParse() {
        String token = AppStoreJwt.create(ISSUER, KEY_ID, AppStoreJwt.parsePrivateKey(pem), Instant.now());

        assertThatThrownBy(() -> client.getJson(token, "/v1/apps", Map.of("limit", "1")))
                .isInstanceOfSatisfying(AppStoreApiException.class, e -> {
                    System.out.printf("[live] apple status=%d code=%s detail=%s%n",
                            e.getStatusCode(), e.getCode(), e.getDetail());
                    assertThat(e.getStatusCode()).isEqualTo(401);
                    assertThat(e.getCode()).isEqualTo("NOT_AUTHORIZED");
                    assertThat(e.getDetail()).isNotBlank();
                });
    }

    @Test
    void connectSurfacesTheRejectionAsAUserMessage() {
        AppStoreIntegrationProvider provider = new AppStoreIntegrationProvider(
                mock(IntegrationRepository.class), mock(IntegrationSecretRepository.class), client, new ObjectMapper());

        assertThatThrownBy(() -> provider.connect(UUID.randomUUID(), "x",
                Map.of("issuerId", ISSUER, "keyId", KEY_ID, "privateKey", pem, "vendorNumber", "85012345")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Apple rejected these credentials");
    }
}
