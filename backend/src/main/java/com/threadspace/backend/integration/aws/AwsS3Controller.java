package com.threadspace.backend.integration.aws;

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
@RequestMapping("/projects/{projectId}/aws/s3")
public class AwsS3Controller {

    private final AwsS3Service awsS3Service;
    private final String internalSyncToken;

    public AwsS3Controller(AwsS3Service awsS3Service,
            @Value("${INTERNAL_SYNC_TOKEN}") String internalSyncToken) {
        this.awsS3Service = awsS3Service;
        this.internalSyncToken = internalSyncToken;
    }

    @GetMapping("/buckets")
    public ResponseEntity<?> getS3Buckets(
            @PathVariable UUID projectId,
            @RequestHeader(value = "x-internal-token", required = false) String token) {

        if (!isValidToken(token)) {
            return ResponseEntity.status(401).body("Unauthorized");
        }

        try {
            List<AwsS3Service.S3BucketInfo> buckets = awsS3Service.getS3Buckets(projectId);
            return ResponseEntity.ok(buckets);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to fetch S3 buckets: " + e.getMessage());
        }
    }

    @GetMapping("/metrics")
    public ResponseEntity<?> getS3Metrics(
            @PathVariable UUID projectId,
            @RequestHeader(value = "x-internal-token", required = false) String token) {

        if (!isValidToken(token)) {
            return ResponseEntity.status(401).body("Unauthorized");
        }

        try {
            AwsS3Service.S3Metrics metrics = awsS3Service.getS3Metrics(projectId);
            return ResponseEntity.ok(metrics);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to fetch S3 metrics: " + e.getMessage());
        }
    }

    private boolean isValidToken(String token) {
        return internalSyncToken != null && internalSyncToken.equals(token);
    }
}
