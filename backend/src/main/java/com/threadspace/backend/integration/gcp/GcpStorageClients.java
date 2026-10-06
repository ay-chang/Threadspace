package com.threadspace.backend.integration.gcp;

import java.util.List;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.threadspace.backend.integration.google.GoogleAccessTokens;
import com.threadspace.backend.integration.google.GoogleServiceAccount;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Builds Cloud Storage clients from stored credentials.
 *
 * Credentials come from {@link GoogleAccessTokens}, so token requests go to the
 * configured Google endpoint - never to the {@code token_uri} inside the
 * uploaded key file, which a crafted file could point anywhere. The Storage
 * host is configurable ({@code gcp.storage-host}) so tests can use a local server.
 */
@Component
public class GcpStorageClients {

    /** The scope StorageOptions applies by default; kept so access is unchanged. */
    static final String SCOPE = "https://www.googleapis.com/auth/cloud-platform";

    private final GoogleAccessTokens accessTokens;
    private final String host;

    public GcpStorageClients(GoogleAccessTokens accessTokens,
            @Value("${gcp.storage-host:https://storage.googleapis.com}") String host) {
        this.accessTokens = accessTokens;
        this.host = host;
    }

    public Storage create(GcpSecretPayload payload) {
        GoogleServiceAccount account = new GoogleServiceAccount(
                payload.projectId(), payload.clientEmail(), payload.serviceAccountJson());
        GoogleCredentials credentials = accessTokens.credentials(account, List.of(SCOPE));

        return StorageOptions.newBuilder()
                .setProjectId(payload.projectId())
                .setCredentials(credentials)
                .setHost(host)
                .build()
                .getService();
    }
}
