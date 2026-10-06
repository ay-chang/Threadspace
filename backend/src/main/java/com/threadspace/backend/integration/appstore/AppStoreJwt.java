package com.threadspace.backend.integration.appstore;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

/**
 * Builds the ES256-signed JWTs the App Store Connect API requires.
 *
 * Apple rejects tokens that live longer than 20 minutes, so each one is minted
 * with a slightly shorter lifetime and never cached beyond a single request.
 */
public final class AppStoreJwt {

    static final String AUDIENCE = "appstoreconnect-v1";
    static final Duration LIFETIME = Duration.ofMinutes(19);

    private static final Base64.Encoder B64URL = Base64.getUrlEncoder().withoutPadding();

    private AppStoreJwt() {
    }

    public static String create(String issuerId, String keyId, ECPrivateKey key, Instant now) {
        String header = "{\"alg\":\"ES256\",\"kid\":\"" + keyId + "\",\"typ\":\"JWT\"}";
        long iat = now.getEpochSecond();
        long exp = now.plus(LIFETIME).getEpochSecond();
        String payload = "{\"iss\":\"" + issuerId + "\",\"iat\":" + iat + ",\"exp\":" + exp
                + ",\"aud\":\"" + AUDIENCE + "\"}";

        String signingInput = b64(header) + "." + b64(payload);
        try {
            // P1363 yields the raw 64-byte R||S signature JWS requires; the
            // default DER encoding would produce a token Apple rejects.
            Signature signer = Signature.getInstance("SHA256withECDSAinP1363Format");
            signer.initSign(key);
            signer.update(signingInput.getBytes(StandardCharsets.US_ASCII));
            return signingInput + "." + B64URL.encodeToString(signer.sign());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to sign App Store Connect token", e);
        }
    }

    /**
     * Parses the contents of an App Store Connect .p8 file. Tolerates the ways
     * the text gets mangled in transit - literal {@code \n} sequences, stray
     * whitespace, a missing header - and rejects anything that isn't a P-256
     * EC private key with a message naming the actual problem.
     */
    public static ECPrivateKey parsePrivateKey(String pem) {
        if (pem == null || pem.isBlank()) {
            throw new IllegalArgumentException("The private key (.p8 file contents) is required.");
        }
        String text = pem.replace("\\n", "\n").trim();

        if (text.contains("PUBLIC KEY")) {
            throw new IllegalArgumentException(
                    "That's a public key. Upload the .p8 private key file you downloaded from App Store Connect.");
        }
        if (text.contains("BEGIN EC PRIVATE KEY") || text.contains("BEGIN RSA PRIVATE KEY")) {
            throw new IllegalArgumentException(
                    "That key isn't in the format App Store Connect issues. Use the original .p8 file "
                            + "(it begins with -----BEGIN PRIVATE KEY-----).");
        }

        String base64 = text
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");

        byte[] der;
        try {
            der = Base64.getDecoder().decode(base64);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "The private key couldn't be read. Upload the .p8 file itself rather than copying part of it.", e);
        }

        PrivateKey key;
        try {
            key = KeyFactory.getInstance("EC").generatePrivate(new PKCS8EncodedKeySpec(der));
        } catch (GeneralSecurityException e) {
            throw new IllegalArgumentException(
                    "The private key couldn't be read. Upload the .p8 file itself rather than copying part of it.", e);
        }

        ECPrivateKey ecKey = (ECPrivateKey) key;
        if (ecKey.getParams().getCurve().getField().getFieldSize() != 256) {
            throw new IllegalArgumentException(
                    "That's an EC key, but not the P-256 kind App Store Connect issues. Use the .p8 file from App Store Connect.");
        }
        return ecKey;
    }

    private static String b64(String json) {
        return B64URL.encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }
}
