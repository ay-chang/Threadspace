package com.threadspace.backend.integration.appstore;

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

import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.threadspace.backend.integration.core.Integration;
import com.threadspace.backend.integration.core.IntegrationRepository;
import com.threadspace.backend.integration.core.IntegrationSecret;
import com.threadspace.backend.integration.core.IntegrationSecretRepository;
import com.threadspace.backend.integration.core.IntegrationStatus;
import com.threadspace.backend.integration.core.IntegrationType;
import com.threadspace.backend.integration.testsupport.TestKeys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AppStoreIntegrationProviderTest {

    private static final UUID PROJECT = UUID.randomUUID();
    private static final String ISSUER = "57246542-96fe-1a63-e053-0824d011072a";
    private static final String KEY_ID = "2X9R4HXF34";
    private static final String VENDOR = "85012345";

    private final ObjectMapper mapper = new ObjectMapper();
    private final String pem = TestKeys.pkcs8Pem(TestKeys.ecKeyPair("secp256r1"));

    private IntegrationRepository integrations;
    private IntegrationSecretRepository secrets;
    private AppStoreApiClient client;
    private AppStoreIntegrationProvider provider;

    @BeforeEach
    void setUp() {
        integrations = mock(IntegrationRepository.class);
        secrets = mock(IntegrationSecretRepository.class);
        client = mock(AppStoreApiClient.class);
        provider = new AppStoreIntegrationProvider(integrations, secrets, client, mapper);

        when(integrations.save(any(Integration.class))).thenAnswer(inv -> {
            Integration i = inv.getArgument(0);
            if (i.getId() == null) {
                i.setId(UUID.randomUUID());
            }
            return i;
        });
        when(secrets.findByIntegrationId(any())).thenReturn(Optional.empty());
        when(secrets.save(any(IntegrationSecret.class))).thenAnswer(inv -> inv.getArgument(0));
        when(client.getJson(anyString(), anyString(), anyMap())).thenReturn(mapper.createObjectNode());
        when(client.getDailySalesReport(anyString(), anyString(), any())).thenReturn(Optional.empty());
    }

    private Map<String, String> credentials() {
        return Map.of("issuerId", ISSUER, "keyId", KEY_ID, "privateKey", pem, "vendorNumber", VENDOR);
    }

    @Test
    void swappedIssuerAndKeyIdsAreCalledOut() {
        assertThatThrownBy(() -> AppStoreIntegrationProvider.validate(KEY_ID, KEY_ID, pem, VENDOR))
                .hasMessageContaining("looks like a Key ID in the Issuer ID field");
        assertThatThrownBy(() -> AppStoreIntegrationProvider.validate(ISSUER, ISSUER, pem, VENDOR))
                .hasMessageContaining("looks like an Issuer ID in the Key ID field");
    }

    @Test
    void malformedFieldsGetSpecificMessages() {
        assertThatThrownBy(() -> AppStoreIntegrationProvider.validate("not-a-uuid", KEY_ID, pem, VENDOR))
                .hasMessageContaining("Issuer ID should look like");
        assertThatThrownBy(() -> AppStoreIntegrationProvider.validate(ISSUER, "abc", pem, VENDOR))
                .hasMessageContaining("Key ID should be 10");
        assertThatThrownBy(() -> AppStoreIntegrationProvider.validate(ISSUER, KEY_ID, pem, "VN-123"))
                .hasMessageContaining("Vendor number should be digits");
        assertThatThrownBy(() -> AppStoreIntegrationProvider.validate(ISSUER, KEY_ID, "nope", VENDOR))
                .hasMessageContaining("couldn't be read");
        assertThatThrownBy(() -> AppStoreIntegrationProvider.validate("", KEY_ID, pem, VENDOR))
                .hasMessageContaining("Issuer ID is required");
    }

    @Test
    void validFieldsAreTrimmed() {
        AppStoreSecretPayload payload = AppStoreIntegrationProvider.validate(
                " " + ISSUER + " ", " " + KEY_ID + "\n", pem, " " + VENDOR + " ");

        assertThat(payload.issuerId()).isEqualTo(ISSUER);
        assertThat(payload.keyId()).isEqualTo(KEY_ID);
        assertThat(payload.vendorNumber()).isEqualTo(VENDOR);
    }

    @Test
    void invalidFieldsNeverReachApple() {
        assertThatThrownBy(() -> provider.connect(PROJECT, "x",
                Map.of("issuerId", KEY_ID, "keyId", KEY_ID, "privateKey", pem, "vendorNumber", VENDOR)))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(client);
        verify(integrations, never()).save(any());
    }

    @Test
    void rejectedCredentialsAreExplainedAndNothingIsSaved() {
        when(client.getJson(anyString(), eq("/v1/apps"), anyMap()))
                .thenThrow(new AppStoreApiException(401, "NOT_AUTHORIZED", "bad token", null));

        assertThatThrownBy(() -> provider.connect(PROJECT, "x", credentials()))
                .hasMessageContaining("Apple rejected these credentials");

        verify(integrations, never()).save(any());
        verify(secrets, never()).save(any());
    }

    @Test
    void keyWithoutSalesRoleIsExplainedAndNothingIsSaved() {
        when(client.getDailySalesReport(anyString(), anyString(), any()))
                .thenThrow(new AppStoreApiException(403, "FORBIDDEN_ERROR", "forbidden", null));

        assertThatThrownBy(() -> provider.connect(PROJECT, "x", credentials()))
                .hasMessageContaining("can't download sales reports")
                .hasMessageContaining("Sales, Finance, or Admin");

        verify(integrations, never()).save(any());
    }

    @Test
    void rejectedVendorNumberIsNamed() {
        when(client.getDailySalesReport(anyString(), anyString(), any()))
                .thenThrow(new AppStoreApiException(400, "PARAMETER_ERROR.INVALID", "invalid vendor", null));

        assertThatThrownBy(() -> provider.connect(PROJECT, "x", credentials()))
                .hasMessageContaining("didn't accept vendor number " + VENDOR);
    }

    @Test
    void successfulConnectSignsWithTheKeyIdAndStoresEverything() throws Exception {
        Integration result = provider.connect(PROJECT, "ignored", credentials());

        assertThat(result.getIntegrationStatus()).isEqualTo(IntegrationStatus.CONNECTED);
        assertThat(result.getIntegrationType()).isEqualTo(IntegrationType.APPLE_CONNECT);
        assertThat(result.getDisplayName()).isEqualTo("App Store Connect");

        // The token sent to Apple must name this key in its header.
        ArgumentCaptor<String> token = ArgumentCaptor.forClass(String.class);
        verify(client).getJson(token.capture(), eq("/v1/apps"), anyMap());
        JsonNode header = mapper.readTree(Base64.getUrlDecoder().decode(token.getValue().split("\\.")[0]));
        assertThat(header.path("kid").asText()).isEqualTo(KEY_ID);

        // The sales probe uses a date Apple has certainly finished processing.
        ArgumentCaptor<LocalDate> probeDate = ArgumentCaptor.forClass(LocalDate.class);
        verify(client).getDailySalesReport(anyString(), eq(VENDOR), probeDate.capture());
        assertThat(probeDate.getValue()).isBefore(LocalDate.now(AppStoreService.REPORT_ZONE).minusDays(1));

        ArgumentCaptor<IntegrationSecret> saved = ArgumentCaptor.forClass(IntegrationSecret.class);
        verify(secrets).save(saved.capture());
        AppStoreSecretPayload payload = mapper.readValue(saved.getValue().getSecretJson(), AppStoreSecretPayload.class);
        assertThat(payload.issuerId()).isEqualTo(ISSUER);
        assertThat(payload.keyId()).isEqualTo(KEY_ID);
        assertThat(payload.vendorNumber()).isEqualTo(VENDOR);
        assertThat(payload.privateKey()).contains("BEGIN PRIVATE KEY");
    }

    @Test
    void displayCredentialsNeverIncludeThePrivateKey() throws Exception {
        stubStoredPayload(new AppStoreSecretPayload(ISSUER, KEY_ID, pem, VENDOR));

        Map<String, String> display = provider.getDisplayCredentials(PROJECT);

        assertThat(display).containsEntry("privateKey", "****").containsEntry("keyId", KEY_ID);
        assertThat(display.values()).noneMatch(v -> v.contains("PRIVATE KEY"));
    }

    @Test
    void updateKeepsFieldsThatWereLeftBlankOrMasked() throws Exception {
        stubStoredPayload(new AppStoreSecretPayload(ISSUER, KEY_ID, pem, VENDOR));

        provider.update(PROJECT, Map.of("vendorNumber", "86099999", "privateKey", "****", "keyId", ""));

        ArgumentCaptor<IntegrationSecret> saved = ArgumentCaptor.forClass(IntegrationSecret.class);
        verify(secrets).save(saved.capture());
        AppStoreSecretPayload payload = mapper.readValue(saved.getValue().getSecretJson(), AppStoreSecretPayload.class);
        assertThat(payload.vendorNumber()).isEqualTo("86099999");
        assertThat(payload.keyId()).isEqualTo(KEY_ID);
        assertThat(payload.privateKey()).isEqualTo(pem.trim());
    }

    private void stubStoredPayload(AppStoreSecretPayload payload) throws Exception {
        Integration integration = new Integration();
        integration.setId(UUID.randomUUID());
        when(integrations.findByProjectIdAndIntegrationType(PROJECT, IntegrationType.APPLE_CONNECT))
                .thenReturn(List.of(integration));
        IntegrationSecret secret = new IntegrationSecret();
        secret.setSecretJson(mapper.writeValueAsString(payload));
        when(secrets.findByIntegrationId(integration.getId())).thenReturn(Optional.of(secret));
    }
}
