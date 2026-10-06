package com.threadspace.backend.integration.appstore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.Signature;
import java.security.interfaces.ECPrivateKey;
import java.time.Instant;
import java.util.Base64;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.threadspace.backend.integration.testsupport.TestKeys;
import org.junit.jupiter.api.Test;

class AppStoreJwtTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String ISSUER = "57246542-96fe-1a63-e053-0824d011072a";
    private static final String KEY_ID = "2X9R4HXF34";

    private final KeyPair pair = TestKeys.ecKeyPair("secp256r1");

    @Test
    void tokenHasAppleRequiredHeaderAndClaims() throws Exception {
        Instant now = Instant.parse("2026-09-27T12:00:00Z");
        ECPrivateKey key = AppStoreJwt.parsePrivateKey(TestKeys.pkcs8Pem(pair));

        String[] parts = AppStoreJwt.create(ISSUER, KEY_ID, key, now).split("\\.");
        assertThat(parts).hasSize(3);

        JsonNode header = decode(parts[0]);
        assertThat(header.path("alg").asText()).isEqualTo("ES256");
        assertThat(header.path("kid").asText()).isEqualTo(KEY_ID);
        assertThat(header.path("typ").asText()).isEqualTo("JWT");

        JsonNode payload = decode(parts[1]);
        assertThat(payload.path("iss").asText()).isEqualTo(ISSUER);
        assertThat(payload.path("aud").asText()).isEqualTo("appstoreconnect-v1");
        assertThat(payload.path("iat").asLong()).isEqualTo(now.getEpochSecond());
        // Apple rejects tokens that live longer than 20 minutes.
        assertThat(payload.path("exp").asLong() - payload.path("iat").asLong())
                .isPositive().isLessThanOrEqualTo(20 * 60);
    }

    @Test
    void signatureIsRawP1363AndVerifiesAgainstThePublicKey() throws Exception {
        ECPrivateKey key = AppStoreJwt.parsePrivateKey(TestKeys.pkcs8Pem(pair));
        String token = AppStoreJwt.create(ISSUER, KEY_ID, key, Instant.now());

        int lastDot = token.lastIndexOf('.');
        byte[] signature = Base64.getUrlDecoder().decode(token.substring(lastDot + 1));
        // JWS ES256 requires exactly R||S, 32 bytes each - not DER.
        assertThat(signature).hasSize(64);

        Signature verifier = Signature.getInstance("SHA256withECDSAinP1363Format");
        verifier.initVerify(pair.getPublic());
        verifier.update(token.substring(0, lastDot).getBytes(StandardCharsets.US_ASCII));
        assertThat(verifier.verify(signature)).isTrue();
    }

    @Test
    void acceptsKeysMangledInTransit() {
        String pem = TestKeys.pkcs8Pem(pair);

        // Literal backslash-n, as when the PEM was stored inside a JSON string.
        assertThat(AppStoreJwt.parsePrivateKey(pem.replace("\n", "\\n"))).isNotNull();
        // Windows line endings and surrounding whitespace.
        assertThat(AppStoreJwt.parsePrivateKey("  \r\n" + pem.replace("\n", "\r\n") + "  ")).isNotNull();
        // Header and footer stripped, bare base64 only.
        String bare = pem.replace("-----BEGIN PRIVATE KEY-----", "").replace("-----END PRIVATE KEY-----", "");
        assertThat(AppStoreJwt.parsePrivateKey(bare)).isNotNull();
    }

    @Test
    void rejectsAPublicKey() {
        assertThatThrownBy(() -> AppStoreJwt.parsePrivateKey(TestKeys.publicPem(pair)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("public key");
    }

    @Test
    void rejectsSec1AndRsaPemHeaders() {
        assertThatThrownBy(() -> AppStoreJwt.parsePrivateKey(TestKeys.pem("EC PRIVATE KEY", new byte[] { 1, 2 })))
                .hasMessageContaining("isn't in the format App Store Connect issues");
        assertThatThrownBy(() -> AppStoreJwt.parsePrivateKey(TestKeys.pem("RSA PRIVATE KEY", new byte[] { 1, 2 })))
                .hasMessageContaining("isn't in the format App Store Connect issues");
    }

    @Test
    void rejectsAnRsaKeyInPkcs8Form() {
        assertThatThrownBy(() -> AppStoreJwt.parsePrivateKey(TestKeys.pkcs8Pem(TestKeys.rsaKeyPair())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("couldn't be read");
    }

    @Test
    void rejectsAnEcKeyOnTheWrongCurve() {
        assertThatThrownBy(() -> AppStoreJwt.parsePrivateKey(TestKeys.pkcs8Pem(TestKeys.ecKeyPair("secp384r1"))))
                .hasMessageContaining("not the P-256 kind");
    }

    @Test
    void rejectsGarbageAndBlank() {
        assertThatThrownBy(() -> AppStoreJwt.parsePrivateKey("AuthKey_2X9R4HXF34.p8"))
                .hasMessageContaining("couldn't be read");
        assertThatThrownBy(() -> AppStoreJwt.parsePrivateKey("   "))
                .hasMessageContaining("required");
    }

    private static JsonNode decode(String part) throws Exception {
        return MAPPER.readTree(Base64.getUrlDecoder().decode(part));
    }
}
