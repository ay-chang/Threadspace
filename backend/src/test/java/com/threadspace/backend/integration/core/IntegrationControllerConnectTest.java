package com.threadspace.backend.integration.core;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Provider validation messages ("That's a publishable key") used to become a
 * bare 500. They must reach the client as a 400 carrying the message.
 */
@WebMvcTest(IntegrationController.class)
@TestPropertySource(properties = "INTERNAL_SYNC_TOKEN=test-token")
class IntegrationControllerConnectTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private IntegrationService service;

    @Test
    void providerValidationMessagesReachTheClient() throws Exception {
        when(service.connectIntegration(any(), any(), anyString(), anyMap()))
                .thenThrow(new IllegalArgumentException("That's a publishable key (pk_...)"));

        mvc.perform(post("/projects/{id}/integrations/connect", UUID.randomUUID())
                .header("x-internal-token", "test-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"integrationType\":\"STRIPE\",\"displayName\":\"Stripe\",\"credentials\":{\"apiKey\":\"pk_live_x\"}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("That's a publishable key (pk_...)"));
    }

    @Test
    void successfulConnectStillReturnsTheIntegration() throws Exception {
        Integration integration = new Integration();
        integration.setId(UUID.randomUUID());
        integration.setIntegrationType(IntegrationType.STRIPE);
        integration.setIntegrationStatus(IntegrationStatus.CONNECTED);
        integration.setDisplayName("Stripe");
        when(service.connectIntegration(any(), any(), anyString(), anyMap())).thenReturn(integration);

        mvc.perform(post("/projects/{id}/integrations/connect", UUID.randomUUID())
                .header("x-internal-token", "test-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"integrationType\":\"STRIPE\",\"displayName\":\"Stripe\",\"credentials\":{\"apiKey\":\"rk_live_x\"}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Stripe"))
                .andExpect(jsonPath("$.integrationType").value("STRIPE"));
    }
}
