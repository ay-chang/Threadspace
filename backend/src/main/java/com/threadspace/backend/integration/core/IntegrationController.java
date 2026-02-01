package com.threadspace.backend.integration.core;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.threadspace.backend.integration.core.IntegrationDtos.IntegrationConnectRequest;
import static com.threadspace.backend.integration.core.IntegrationDtos.IntegrationUpdateRequest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.threadspace.backend.integration.core.IntegrationDtos.IntegrationCreateRequest;
import com.threadspace.backend.integration.core.IntegrationDtos.IntegrationResponse;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@RestController
@RequestMapping("/projects/{projectId}/integrations")
public class IntegrationController {

    private final IntegrationService integrationService;

    @Value("${INTERNAL_SYNC_TOKEN}")
    private String internalSyncToken;

    public IntegrationController(IntegrationService integrationService) {
        this.integrationService = integrationService;
    }

    @PostMapping
    public ResponseEntity<IntegrationResponse> createIntegration(
            @PathVariable("projectId") UUID projectId,
            @RequestBody IntegrationCreateRequest body,
            @RequestHeader(name = "x-internal-token", required = false) String token) {

        if (token == null || !token.equals(internalSyncToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Integration integration = integrationService.createIntegration(
                body.displayName(),
                body.integrationType(),
                projectId);

        IntegrationResponse response = new IntegrationResponse(
                integration.getId(),
                integration.getProjectId(),
                integration.getIntegrationType(),
                integration.getIntegrationStatus(),
                integration.getDisplayName(),
                integration.getCreatedAt(),
                integration.getUpdatedAt());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/connect")
    public ResponseEntity<IntegrationResponse> connectIntegration(
            @PathVariable("projectId") UUID projectId,
            @RequestBody IntegrationConnectRequest body,
            @RequestHeader(name = "x-internal-token", required = false) String token) {

        if (token == null || !token.equals(internalSyncToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Integration integration = integrationService.connectIntegration(
                projectId,
                body.integrationType(),
                body.displayName(),
                body.credentials());

        IntegrationResponse response = new IntegrationResponse(
                integration.getId(),
                integration.getProjectId(),
                integration.getIntegrationType(),
                integration.getIntegrationStatus(),
                integration.getDisplayName(),
                integration.getCreatedAt(),
                integration.getUpdatedAt());

        return ResponseEntity.ok(response);
    }

    @PutMapping("/update")
    public ResponseEntity<IntegrationResponse> updateIntegration(
            @PathVariable("projectId") UUID projectId,
            @RequestBody IntegrationUpdateRequest body,
            @RequestHeader(name = "x-internal-token", required = false) String token) {

        if (token == null || !token.equals(internalSyncToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Integration integration = integrationService.updateIntegration(
                projectId,
                body.integrationType(),
                body.credentials());

        IntegrationResponse response = new IntegrationResponse(
                integration.getId(),
                integration.getProjectId(),
                integration.getIntegrationType(),
                integration.getIntegrationStatus(),
                integration.getDisplayName(),
                integration.getCreatedAt(),
                integration.getUpdatedAt());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/credentials/{integrationType}")
    public ResponseEntity<Map<String, String>> getCredentials(
            @PathVariable("projectId") UUID projectId,
            @PathVariable("integrationType") IntegrationType integrationType,
            @RequestHeader(name = "x-internal-token", required = false) String token) {

        if (token == null || !token.equals(internalSyncToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Map<String, String> credentials = integrationService.getDisplayCredentials(projectId, integrationType);
        return ResponseEntity.ok(credentials);
    }

    @GetMapping
    public ResponseEntity<List<IntegrationResponse>> listIntegrationsForProject(
            @PathVariable("projectId") UUID projectId,
            @RequestHeader(name = "x-internal-token", required = false) String token) {

        if (token == null || !token.equals(internalSyncToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<IntegrationResponse> responses = integrationService
                .getIntegrationsForProject(projectId)
                .stream()
                .map(integration -> new IntegrationResponse(
                        integration.getId(),
                        integration.getProjectId(),
                        integration.getIntegrationType(),
                        integration.getIntegrationStatus(),
                        integration.getDisplayName(),
                        integration.getCreatedAt(),
                        integration.getUpdatedAt()))
                .toList();

        return ResponseEntity.ok(responses);
    }

}
