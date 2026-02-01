package com.threadspace.backend.integration.aws;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/projects/{projectId}/aws/rds")
public class AwsRdsController {

    private final AwsRdsService awsRdsService;
    private final String internalSyncToken;

    public AwsRdsController(AwsRdsService awsRdsService) {
        this.awsRdsService = awsRdsService;
        this.internalSyncToken = System.getenv("INTERNAL_SYNC_TOKEN");
    }

    @GetMapping("/instances")
    public ResponseEntity<?> getRdsInstances(
            @PathVariable UUID projectId,
            @RequestHeader(value = "x-internal-token", required = false) String token) {

        if (!isValidToken(token)) {
            return ResponseEntity.status(401).body("Unauthorized");
        }

        try {
            List<AwsRdsService.RdsInstanceInfo> instances = awsRdsService.getRdsInstances(projectId);
            return ResponseEntity.ok(instances);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to fetch RDS instances: " + e.getMessage());
        }
    }

    @GetMapping("/metrics")
    public ResponseEntity<?> getRdsMetrics(
            @PathVariable UUID projectId,
            @RequestHeader(value = "x-internal-token", required = false) String token) {

        if (!isValidToken(token)) {
            return ResponseEntity.status(401).body("Unauthorized");
        }

        try {
            AwsRdsService.RdsMetrics metrics = awsRdsService.getRdsMetrics(projectId);
            return ResponseEntity.ok(metrics);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to fetch RDS metrics: " + e.getMessage());
        }
    }

    private boolean isValidToken(String token) {
        return internalSyncToken != null && internalSyncToken.equals(token);
    }
}
