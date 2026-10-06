package com.threadspace.backend.integration.googleanalytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.threadspace.backend.integration.core.Integration;
import com.threadspace.backend.integration.core.IntegrationRepository;
import com.threadspace.backend.integration.core.IntegrationSecret;
import com.threadspace.backend.integration.core.IntegrationSecretRepository;
import com.threadspace.backend.integration.core.IntegrationStatus;
import com.threadspace.backend.integration.core.IntegrationType;
import com.threadspace.backend.integration.google.GoogleAccessTokens;
import com.threadspace.backend.integration.google.GoogleTokenException;
import com.threadspace.backend.integration.testsupport.TestKeys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

class GoogleAnalyticsIntegrationProviderTest {

    private static final UUID PROJECT = UUID.randomUUID();
    private static final String EMAIL = "threadspace@my-project.iam.gserviceaccount.com";

    private final ObjectMapper mapper = new ObjectMapper();
    private final String keyJson = TestKeys.serviceAccountJson("my-project", EMAIL, "https://oauth2.googleapis.com/token");

    private IntegrationRepository integrations;
    private IntegrationSecretRepository secrets;
    private GoogleAccessTokens tokens;
    private GoogleAnalyticsApiClient client;
    private GoogleAnalyticsIntegrationProvider provider;

    @BeforeEach
    void setUp() {
        integrations = mock(IntegrationRepository.class);
        secrets = mock(IntegrationSecretRepository.class);
        tokens = mock(GoogleAccessTokens.class);
        client = mock(GoogleAnalyticsApiClient.class);
        provider = new GoogleAnalyticsIntegrationProvider(integrations, secrets, tokens, client, mapper);

        when(integrations.save(any(Integration.class))).thenAnswer(inv -> {
            Integration i = inv.getArgument(0);
            if (i.getId() == null) {
                i.setId(UUID.randomUUID());
            }
            return i;
        });
        when(secrets.findByIntegrationId(any())).thenReturn(Optional.empty());
        when(secrets.save(any(IntegrationSecret.class))).thenAnswer(inv -> inv.getArgument(0));
        when(tokens.accessToken(any(), anyString())).thenReturn("ya29.token");
        when(client.runReport(anyString(), anyString(), any())).thenReturn(mapper.createObjectNode());
    }

    private Map<String, String> credentials(String propertyId) {
        return Map.of("propertyId", propertyId, "serviceAccountJson", keyJson);
    }

    @Test
    void measurementIdIsNamedAndNothingIsCalled() {
        assertThatThrownBy(() -> provider.connect(PROJECT, "x", credentials("G-ABC123XYZ")))
                .hasMessageContaining("That's a Measurement ID");

        verifyNoInteractions(tokens, client);
        verify(integrations, never()).save(any());
    }

    @Test
    void universalAnalyticsIdIsNamed() {
        assertThatThrownBy(() -> GoogleAnalyticsIntegrationProvider.validatePropertyId("UA-12345-1"))
                .hasMessageContaining("Universal Analytics has been shut down");
    }

    @ParameterizedTest
    @ValueSource(strings = { "abc", "12 34", "1234", "properties/abc" })
    void nonNumericPropertyIdsAreRejected(String value) {
        assertThatThrownBy(() -> GoogleAnalyticsIntegrationProvider.validatePropertyId(value))
                .hasMessageContaining("digits only");
    }

    @Test
    void acceptsTheResourceNameForm() {
        assertThat(GoogleAnalyticsIntegrationProvider.validatePropertyId(" properties/123456789 "))
                .isEqualTo("123456789");
    }

    @Test
    void aRejectedKeyNamesTheAccountAndSavesNothing() {
        when(tokens.accessToken(any(), anyString()))
                .thenThrow(new GoogleTokenException(true, "invalid_grant", null));

        assertThatThrownBy(() -> provider.connect(PROJECT, "x", credentials("123456789")))
                .hasMessageContaining("Google rejected this service account key")
                .hasMessageContaining(EMAIL);

        verify(integrations, never()).save(any());
    }

    @Test
    void aDisabledApiSaysWhereToEnableIt() {
        when(client.runReport(anyString(), anyString(), any()))
                .thenThrow(new GoogleAnalyticsApiException(403, "PERMISSION_DENIED", "SERVICE_DISABLED", "disabled", null));

        assertThatThrownBy(() -> provider.connect(PROJECT, "x", credentials("123456789")))
                .hasMessageContaining("Data API isn't enabled for Google Cloud project my-project")
                .hasMessageContaining("analyticsdata.googleapis.com?project=my-project");

        verify(integrations, never()).save(any());
    }

    @Test
    void aMissingPropertyGrantSaysExactlyWhoToAdd() {
        when(client.runReport(anyString(), anyString(), any()))
                .thenThrow(new GoogleAnalyticsApiException(403, "PERMISSION_DENIED", null,
                        "User does not have sufficient permissions for this property.", null));

        assertThatThrownBy(() -> provider.connect(PROJECT, "x", credentials("123456789")))
                .hasMessageContaining("The service account " + EMAIL + " can't read GA4 property 123456789")
                .hasMessageContaining("Viewer role");

        verify(integrations, never()).save(any());
    }

    @Test
    void anUnknownPropertyIsCalledOut() {
        when(client.runReport(anyString(), anyString(), any()))
                .thenThrow(new GoogleAnalyticsApiException(400, "INVALID_ARGUMENT", null, "Invalid property", null));

        assertThatThrownBy(() -> provider.connect(PROJECT, "x", credentials("123456789")))
                .hasMessageContaining("didn't recognise property 123456789");
    }

    @Test
    void successfulConnectProbesThePropertyAndStoresEverything() throws Exception {
        Integration result = provider.connect(PROJECT, "ignored", credentials("properties/123456789"));

        assertThat(result.getIntegrationType()).isEqualTo(IntegrationType.GOOGLE_ANALYTICS);
        assertThat(result.getIntegrationStatus()).isEqualTo(IntegrationStatus.CONNECTED);
        assertThat(result.getDisplayName()).isEqualTo("Google Analytics");
        verify(client).runReport(eq("ya29.token"), eq("123456789"), any());

        ArgumentCaptor<IntegrationSecret> saved = ArgumentCaptor.forClass(IntegrationSecret.class);
        verify(secrets).save(saved.capture());
        assertThat(saved.getValue().getProvider()).isEqualTo(IntegrationType.GOOGLE_ANALYTICS);
        GoogleAnalyticsSecretPayload payload = mapper.readValue(saved.getValue().getSecretJson(),
                GoogleAnalyticsSecretPayload.class);
        assertThat(payload.propertyId()).isEqualTo("123456789");
        assertThat(payload.projectId()).isEqualTo("my-project");
        assertThat(payload.clientEmail()).isEqualTo(EMAIL);
        assertThat(payload.serviceAccountJson()).isEqualTo(keyJson);
    }

    @Test
    void displayCredentialsNeverIncludeTheKey() throws Exception {
        Integration integration = new Integration();
        integration.setId(UUID.randomUUID());
        when(integrations.findByProjectIdAndIntegrationType(PROJECT, IntegrationType.GOOGLE_ANALYTICS))
                .thenReturn(List.of(integration));
        IntegrationSecret secret = new IntegrationSecret();
        secret.setSecretJson(mapper.writeValueAsString(
                new GoogleAnalyticsSecretPayload("123456789", "my-project", EMAIL, keyJson)));
        when(secrets.findByIntegrationId(integration.getId())).thenReturn(Optional.of(secret));

        Map<String, String> display = provider.getDisplayCredentials(PROJECT);

        assertThat(display).containsEntry("propertyId", "123456789")
                .containsEntry("clientEmail", EMAIL)
                .containsEntry("serviceAccountJson", "****");
        assertThat(display.values()).noneMatch(v -> v.contains("PRIVATE KEY"));
    }
}
