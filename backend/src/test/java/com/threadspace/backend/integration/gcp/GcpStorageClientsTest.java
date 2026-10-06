package com.threadspace.backend.integration.gcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import com.threadspace.backend.integration.core.Integration;
import com.threadspace.backend.integration.core.IntegrationRepository;
import com.threadspace.backend.integration.core.IntegrationSecret;
import com.threadspace.backend.integration.core.IntegrationSecretRepository;
import com.threadspace.backend.integration.core.IntegrationStatus;
import com.threadspace.backend.integration.core.IntegrationType;
import com.threadspace.backend.integration.google.GoogleAccessTokens;
import com.threadspace.backend.integration.testsupport.StubServer;
import com.threadspace.backend.integration.testsupport.StubServer.Request;
import com.threadspace.backend.integration.testsupport.StubServer.Response;
import com.threadspace.backend.integration.testsupport.TestKeys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * GCP against one local server playing both Google's token endpoint and the
 * Cloud Storage JSON API. The key files used here point their token_uri at an
 * unresolvable host: if that URL were ever used, the request would fail.
 */
class GcpStorageClientsTest {

    private static final String EMAIL = "threadspace@my-project.iam.gserviceaccount.com";
    private static final String HOSTILE_TOKEN_URI = "https://attacker.invalid/token";

    private final ObjectMapper mapper = new ObjectMapper();
    private final String keyJson = TestKeys.serviceAccountJson("my-project", EMAIL, HOSTILE_TOKEN_URI);
    private final GcpSecretPayload payload = new GcpSecretPayload("my-project", EMAIL, keyJson);

    private IntegrationRepository integrations;
    private IntegrationSecretRepository secrets;

    @BeforeEach
    void setUp() {
        integrations = mock(IntegrationRepository.class);
        secrets = mock(IntegrationSecretRepository.class);
        when(integrations.save(any(Integration.class))).thenAnswer(inv -> {
            Integration i = inv.getArgument(0);
            if (i.getId() == null) {
                i.setId(UUID.randomUUID());
            }
            return i;
        });
        when(secrets.findByIntegrationId(any())).thenReturn(Optional.empty());
    }

    /** Token endpoint plus a project with one bucket holding two objects. */
    private static Function<Request, Response> fakeGoogle() {
        return req -> {
            if (req.path().equals("/token")) {
                return Response.json(200, "{\"access_token\":\"ya29.gcp\",\"expires_in\":3599,\"token_type\":\"Bearer\"}");
            }
            if (!"Bearer ya29.gcp".equals(req.header("Authorization"))) {
                return Response.json(401, "{\"error\":{\"code\":401,\"message\":\"Invalid Credentials\"}}");
            }
            if (req.path().equals("/storage/v1/b")) {
                return Response.json(200, """
                        {"kind":"storage#buckets","items":[{"kind":"storage#bucket","id":"bucket-a","name":"bucket-a",
                          "location":"US","timeCreated":"2026-01-01T00:00:00.000Z"}]}""");
            }
            if (req.path().equals("/storage/v1/b/bucket-a/o")) {
                return Response.json(200, """
                        {"kind":"storage#objects","items":[
                          {"kind":"storage#object","name":"a.txt","bucket":"bucket-a","size":"100"},
                          {"kind":"storage#object","name":"b.txt","bucket":"bucket-a","size":"250"}]}""");
            }
            return Response.json(404, "{\"error\":{\"code\":404,\"message\":\"Not Found\"}}");
        };
    }

    private GcpStorageClients clients(StubServer google) {
        return new GcpStorageClients(new GoogleAccessTokens(google.baseUrl() + "/token"), google.baseUrl());
    }

    @Test
    void tokenRequestsGoToTheConfiguredEndpointNotTheKeyFiles() throws Exception {
        try (StubServer google = new StubServer(fakeGoogle())) {
            Storage storage = clients(google).create(payload);

            List<String> names = new ArrayList<>();
            for (Bucket bucket : storage.list().iterateAll()) {
                names.add(bucket.getName());
            }

            assertThat(names).containsExactly("bucket-a");
            assertThat(google.requests()).extracting(Request::path).contains("/token", "/storage/v1/b");
            assertThat(google.requests()).filteredOn(r -> r.path().startsWith("/storage/"))
                    .allSatisfy(r -> assertThat(r.header("Authorization")).isEqualTo("Bearer ya29.gcp"));
            assertThat(google.requests()).noneMatch(r -> r.body().contains("attacker.invalid"));
        }
    }

    @Test
    void connectVerifiesAgainstStorageAndSavesTheKey() throws Exception {
        try (StubServer google = new StubServer(fakeGoogle())) {
            GcpIntegrationProvider provider = new GcpIntegrationProvider(integrations, secrets, mapper, clients(google));

            Integration result = provider.connect(UUID.randomUUID(), "x", Map.of("serviceAccountJson", keyJson));

            assertThat(result.getIntegrationStatus()).isEqualTo(IntegrationStatus.CONNECTED);
            assertThat(result.getDisplayName()).isEqualTo("my-project");
            verify(secrets).save(any(IntegrationSecret.class));
        }
    }

    @Test
    void aRejectedKeyFailsConnectAndSavesNothing() throws Exception {
        try (StubServer google = new StubServer(req -> Response.json(400,
                "{\"error\":\"invalid_grant\",\"error_description\":\"Invalid JWT Signature.\"}"))) {
            GcpIntegrationProvider provider = new GcpIntegrationProvider(integrations, secrets, mapper, clients(google));

            assertThatThrownBy(() -> provider.connect(UUID.randomUUID(), "x", Map.of("serviceAccountJson", keyJson)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid GCP credentials");

            verify(integrations, never()).save(any());
            verify(secrets, never()).save(any());
        }
    }

    @Test
    void metricsServiceTotalsBucketsAndObjects() throws Exception {
        Integration integration = new Integration();
        integration.setId(UUID.randomUUID());
        integration.setIntegrationStatus(IntegrationStatus.CONNECTED);
        UUID project = UUID.randomUUID();
        when(integrations.findByProjectIdAndIntegrationType(project, IntegrationType.GOOGLE_CLOUD))
                .thenReturn(List.of(integration));
        IntegrationSecret secret = new IntegrationSecret();
        secret.setSecretJson(mapper.writeValueAsString(payload));
        when(secrets.findByIntegrationId(integration.getId())).thenReturn(Optional.of(secret));

        try (StubServer google = new StubServer(fakeGoogle())) {
            GcpStorageService service = new GcpStorageService(integrations, secrets, mapper, clients(google));

            GcpStorageService.StorageMetrics metrics = service.getStorageMetrics(project);

            assertThat(metrics.totalBuckets()).isEqualTo(1);
            assertThat(metrics.totalObjects()).isEqualTo(2);
            assertThat(metrics.buckets().get(0).sizeBytes()).isEqualTo(350);
            assertThat(metrics.buckets().get(0).location()).isEqualTo("US");
        }
    }
}
