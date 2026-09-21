package com.threadspace.backend.integration.gcp;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.gax.paging.Page;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import com.threadspace.backend.integration.core.Integration;
import com.threadspace.backend.integration.core.IntegrationRepository;
import com.threadspace.backend.integration.core.IntegrationSecret;
import com.threadspace.backend.integration.core.IntegrationSecretRepository;
import com.threadspace.backend.integration.core.IntegrationStatus;
import com.threadspace.backend.integration.core.IntegrationType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GcpStorageService {

    private final IntegrationRepository integrationRepository;
    private final IntegrationSecretRepository integrationSecretRepository;
    private final ObjectMapper objectMapper;

    public GcpStorageService(IntegrationRepository integrationRepository,
            IntegrationSecretRepository integrationSecretRepository,
            ObjectMapper objectMapper) {
        this.integrationRepository = integrationRepository;
        this.integrationSecretRepository = integrationSecretRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<GcsBucketInfo> getStorageBuckets(UUID projectId) {
        GcpSecretPayload credentials = getGcpCredentials(projectId);

        try {
            Storage storage = GcpIntegrationProvider.createStorageClient(credentials);
            Page<Bucket> buckets = storage.list();

            List<GcsBucketInfo> result = new ArrayList<>();
            for (Bucket bucket : buckets.iterateAll()) {
                long objectCount = 0;
                long totalSize = 0;

                try {
                    Page<Blob> blobs = storage.list(bucket.getName(),
                            Storage.BlobListOption.pageSize(1000));
                    for (Blob blob : blobs.iterateAll()) {
                        objectCount++;
                        totalSize += blob.getSize();
                    }
                } catch (Exception e) {
                    System.err.println("Could not get details for bucket: " + bucket.getName());
                }

                GcsBucketInfo info = new GcsBucketInfo(
                        bucket.getName(),
                        bucket.getCreateTimeOffsetDateTime() != null
                                ? bucket.getCreateTimeOffsetDateTime().toString()
                                : "",
                        objectCount,
                        totalSize,
                        bucket.getLocation() != null ? bucket.getLocation() : "");
                result.add(info);
            }

            return result;
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch GCS buckets: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public StorageMetrics getStorageMetrics(UUID projectId) {
        List<GcsBucketInfo> buckets = getStorageBuckets(projectId);

        int totalBuckets = buckets.size();
        long totalObjects = 0;
        long totalStorageBytes = 0;

        for (GcsBucketInfo bucket : buckets) {
            totalObjects += bucket.objectCount();
            totalStorageBytes += bucket.sizeBytes();
        }

        double totalStorageGB = totalStorageBytes / (1024.0 * 1024.0 * 1024.0);

        return new StorageMetrics(
                totalBuckets,
                totalObjects,
                totalStorageGB,
                buckets);
    }

    private GcpSecretPayload getGcpCredentials(UUID projectId) {
        Integration integration = integrationRepository
                .findByProjectIdAndIntegrationType(projectId, IntegrationType.GOOGLE_CLOUD)
                .stream()
                .filter(i -> i.getIntegrationStatus() == IntegrationStatus.CONNECTED)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No connected Google Cloud integration found for project"));

        IntegrationSecret secret = integrationSecretRepository
                .findByIntegrationId(integration.getId())
                .orElseThrow(() -> new IllegalArgumentException("GCP credentials not found"));

        try {
            return objectMapper.readValue(secret.getSecretJson(), GcpSecretPayload.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse GCP credentials", e);
        }
    }

    public record GcsBucketInfo(
            String name,
            String creationDate,
            long objectCount,
            long sizeBytes,
            String location) {
    }

    public record StorageMetrics(
            int totalBuckets,
            long totalObjects,
            double totalStorageGB,
            List<GcsBucketInfo> buckets) {
    }
}
