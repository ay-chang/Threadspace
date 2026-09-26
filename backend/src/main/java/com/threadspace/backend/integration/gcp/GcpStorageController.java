package com.threadspace.backend.integration.gcp;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/projects/{projectId}/gcp/storage")
public class GcpStorageController {

    private final GcpStorageService gcpStorageService;

    @Value("${INTERNAL_SYNC_TOKEN}")
    private String internalSyncToken;

    public GcpStorageController(GcpStorageService gcpStorageService) {
        this.gcpStorageService = gcpStorageService;
    }

    @GetMapping("/buckets")
    public ResponseEntity<?> getStorageBuckets(
            @PathVariable UUID projectId,
            @RequestHeader(value = "x-internal-token", required = false) String token) {

        if (!isValidToken(token)) {
            return ResponseEntity.status(401).body("Unauthorized");
        }

        try {
            List<GcpStorageService.GcsBucketInfo> buckets = gcpStorageService.getStorageBuckets(projectId);
            return ResponseEntity.ok(buckets);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to fetch GCS buckets: " + e.getMessage());
        }
    }

    @GetMapping("/metrics")
    public ResponseEntity<?> getStorageMetrics(
            @PathVariable UUID projectId,
            @RequestHeader(value = "x-internal-token", required = false) String token) {

        if (!isValidToken(token)) {
            return ResponseEntity.status(401).body("Unauthorized");
        }

        try {
            GcpStorageService.StorageMetrics metrics = gcpStorageService.getStorageMetrics(projectId);
            return ResponseEntity.ok(metrics);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to fetch GCS metrics: " + e.getMessage());
        }
    }

    private boolean isValidToken(String token) {
        return internalSyncToken != null && internalSyncToken.equals(token);
    }
}
