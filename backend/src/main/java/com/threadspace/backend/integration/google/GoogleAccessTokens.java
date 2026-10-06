package com.threadspace.backend.integration.google;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Exchanges a service account key for a short-lived OAuth access token. The
 * token endpoint is configuration, not taken from the key file - see
 * {@link GoogleServiceAccount#credentials}.
 */
@Component
public class GoogleAccessTokens {

    private final URI tokenServerUri;

    public GoogleAccessTokens(@Value("${google.token-uri:https://oauth2.googleapis.com/token}") String tokenServerUri) {
        this.tokenServerUri = URI.create(tokenServerUri);
    }

    /**
     * Credentials for {@code account} that fetch tokens from the configured
     * endpoint. Every Google integration must build credentials here rather
     * than with {@code GoogleCredentials.fromStream}, which would honour the
     * uploaded file's own token_uri.
     */
    public GoogleCredentials credentials(GoogleServiceAccount account, List<String> scopes) {
        try {
            return account.credentials(tokenServerUri, scopes);
        } catch (IOException e) {
            throw new IllegalArgumentException("The service account key couldn't be read: " + e.getMessage(), e);
        }
    }

    public String accessToken(GoogleServiceAccount account, String scope) {
        try {
            GoogleCredentials credentials = credentials(account, List.of(scope));
            AccessToken token = credentials.refreshAccessToken();
            return token.getTokenValue();
        } catch (IOException e) {
            String message = String.valueOf(e.getMessage());
            // The auth library reports token endpoint refusals as an IOException
            // quoting the HTTP status and Google's OAuth error body.
            boolean rejected = message.contains("invalid_grant")
                    || message.contains("invalid_client")
                    || message.contains("unauthorized_client")
                    || message.matches("(?s).*\\b(400|401|403)\\b.*");
            throw new GoogleTokenException(rejected, message, e);
        }
    }
}
