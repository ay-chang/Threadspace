package com.threadspace.backend.integration.stripe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

class StripeIntegrationProviderTest {

    private static final UUID PROJECT = UUID.randomUUID();

    private final ObjectMapper mapper = new ObjectMapper();
    private IntegrationRepository integrations;
    private IntegrationSecretRepository secrets;
    private StripeApiClient client;
    private StripeIntegrationProvider provider;

    @BeforeEach
    void setUp() {
        integrations = mock(IntegrationRepository.class);
        secrets = mock(IntegrationSecretRepository.class);
        client = mock(StripeApiClient.class);
        provider = new StripeIntegrationProvider(integrations, secrets, client, mapper);

        when(integrations.save(any(Integration.class))).thenAnswer(inv -> {
            Integration i = inv.getArgument(0);
            if (i.getId() == null) {
                i.setId(UUID.randomUUID());
            }
            return i;
        });
        when(secrets.findByIntegrationId(any())).thenReturn(Optional.empty());
        when(secrets.save(any(IntegrationSecret.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void publishableKeyIsNamedAndNothingIsCalledOrSaved() {
        assertThatThrownBy(() -> provider.connect(PROJECT, "Stripe", Map.of("apiKey", "pk_live_123456789")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("publishable key");

        verifyNoInteractions(client);
        verify(integrations, never()).save(any());
        verify(secrets, never()).save(any());
    }

    @Test
    void webhookSecretIsNamed() {
        assertThatThrownBy(() -> StripeIntegrationProvider.validateKeyFormat("whsec_abcdef"))
                .hasMessageContaining("webhook signing secret");
    }

    @ParameterizedTest
    @ValueSource(strings = { "hello", "sk_abc", "rk_prod_abc", "sk_live_has space", "acct_123" })
    void unrecognisedKeysAreRejected(String key) {
        assertThatThrownBy(() -> StripeIntegrationProvider.validateKeyFormat(key))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("doesn't look like a Stripe API key");
    }

    @Test
    void blankKeyIsRequired() {
        assertThatThrownBy(() -> StripeIntegrationProvider.validateKeyFormat("   "))
                .hasMessageContaining("required");
    }

    @Test
    void validKeysAreTrimmed() {
        assertThat(StripeIntegrationProvider.validateKeyFormat("  rk_test_abc123  ")).isEqualTo("rk_test_abc123");
        assertThat(StripeIntegrationProvider.validateKeyFormat("sk_live_abc123")).isEqualTo("sk_live_abc123");
    }

    @Test
    void missingPermissionIsNamedAndNothingIsSaved() {
        when(client.get(anyString(), eq("/v1/subscriptions"), anyMap()))
                .thenThrow(new StripeApiException(403, "invalid_request_error", null,
                        "The provided key does not have the required permissions", null));

        assertThatThrownBy(() -> provider.connect(PROJECT, "Stripe", Map.of("apiKey", "rk_live_abc123456")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("permission to read Subscriptions")
                .hasMessageContaining("set Subscriptions to Read");

        verify(integrations, never()).save(any());
        verify(secrets, never()).save(any());
    }

    @Test
    void rejectedKeySaysSoAndNothingIsSaved() {
        when(client.get(anyString(), eq("/v1/balance"), anyMap()))
                .thenThrow(new StripeApiException(401, "invalid_request_error", "api_key_invalid",
                        "Invalid API Key provided", null));

        assertThatThrownBy(() -> provider.connect(PROJECT, "Stripe", Map.of("apiKey", "sk_test_abc123456")))
                .hasMessageContaining("Stripe rejected this key");

        verify(integrations, never()).save(any());
    }

    @Test
    void successfulConnectProbesEveryEndpointAndStoresTheKey() throws Exception {
        Integration result = provider.connect(PROJECT, "ignored", Map.of("apiKey", " rk_test_abc123456 "));

        for (String path : List.of("/v1/balance", "/v1/charges", "/v1/subscriptions", "/v1/customers")) {
            verify(client).get(eq("rk_test_abc123456"), eq(path), anyMap());
        }

        assertThat(result.getIntegrationStatus()).isEqualTo(IntegrationStatus.CONNECTED);
        assertThat(result.getIntegrationType()).isEqualTo(IntegrationType.STRIPE);
        assertThat(result.getDisplayName()).isEqualTo("Stripe (test mode)");

        ArgumentCaptor<IntegrationSecret> saved = ArgumentCaptor.forClass(IntegrationSecret.class);
        verify(secrets).save(saved.capture());
        StripeSecretPayload payload = mapper.readValue(saved.getValue().getSecretJson(), StripeSecretPayload.class);
        assertThat(payload.apiKey()).isEqualTo("rk_test_abc123456");
        assertThat(payload.livemode()).isFalse();
        assertThat(saved.getValue().getProvider()).isEqualTo(IntegrationType.STRIPE);
    }

    @Test
    void liveKeyIsLabelledLive() {
        Integration result = provider.connect(PROJECT, "ignored", Map.of("apiKey", "rk_live_abc123456"));
        assertThat(result.getDisplayName()).isEqualTo("Stripe");
    }

    @Test
    void displayCredentialsMaskTheKeyButKeepTheMode() throws Exception {
        Integration integration = new Integration();
        integration.setId(UUID.randomUUID());
        when(integrations.findByProjectIdAndIntegrationType(PROJECT, IntegrationType.STRIPE))
                .thenReturn(List.of(integration));
        IntegrationSecret secret = new IntegrationSecret();
        secret.setSecretJson(mapper.writeValueAsString(new StripeSecretPayload("rk_live_abcdefgh1234", true)));
        when(secrets.findByIntegrationId(integration.getId())).thenReturn(Optional.of(secret));

        Map<String, String> display = provider.getDisplayCredentials(PROJECT);

        assertThat(display).containsEntry("apiKey", "rk_live_****1234").containsEntry("mode", "live");
    }

    @Test
    void unexpectedErrorsIncludeStripesMessage() {
        String message = StripeIntegrationProvider.describeVerificationFailure(
                new StripeApiException(500, "api_error", null, "Something went wrong", null), "Balance");
        assertThat(message).contains("Something went wrong");
    }
}
