package com.threadspace.backend.integration.testsupport;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;
import java.util.zip.GZIPOutputStream;

/** Generates keys and fixtures shaped like what Apple hands out. */
public final class TestKeys {

    private TestKeys() {
    }

    public static KeyPair ecKeyPair(String curve) {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
            generator.initialize(new ECGenParameterSpec(curve));
            return generator.generateKeyPair();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static KeyPair rsaKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** PEM text in the exact layout of an App Store Connect .p8 file. */
    public static String pkcs8Pem(KeyPair pair) {
        return pem("PRIVATE KEY", pair.getPrivate().getEncoded());
    }

    public static String publicPem(KeyPair pair) {
        return pem("PUBLIC KEY", pair.getPublic().getEncoded());
    }

    public static String pem(String label, byte[] der) {
        String body = Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(der);
        return "-----BEGIN " + label + "-----\n" + body + "\n-----END " + label + "-----\n";
    }

    /**
     * A structurally real service account key file, signed-token capable, for
     * an account that doesn't exist. {@code tokenUri} is what the file claims.
     */
    public static String serviceAccountJson(String projectId, String clientEmail, String tokenUri) {
        String pem = pkcs8Pem(rsaKeyPair()).replace("\n", "\\n");
        return "{"
                + "\"type\": \"service_account\","
                + "\"project_id\": \"" + projectId + "\","
                + "\"private_key_id\": \"0123456789abcdef0123456789abcdef01234567\","
                + "\"private_key\": \"" + pem + "\","
                + "\"client_email\": \"" + clientEmail + "\","
                + "\"client_id\": \"104350000000000000000\","
                + "\"auth_uri\": \"https://accounts.google.com/o/oauth2/auth\","
                + "\"token_uri\": \"" + tokenUri + "\""
                + "}";
    }

    public static byte[] gzip(byte[] content) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
                GZIPOutputStream gz = new GZIPOutputStream(out)) {
            gz.write(content);
            gz.finish();
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
