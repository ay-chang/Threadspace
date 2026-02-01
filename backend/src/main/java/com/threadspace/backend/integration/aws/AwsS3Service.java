package com.threadspace.backend.integration.aws;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.threadspace.backend.integration.core.Integration;
import com.threadspace.backend.integration.core.IntegrationRepository;
import com.threadspace.backend.integration.core.IntegrationSecret;
import com.threadspace.backend.integration.core.IntegrationSecretRepository;
import com.threadspace.backend.integration.core.IntegrationStatus;
import com.threadspace.backend.integration.core.IntegrationType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.ListBucketsResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;

@Service
public class AwsS3Service {

    private final IntegrationRepository integrationRepository;
    private final IntegrationSecretRepository integrationSecretRepository;
    private final ObjectMapper objectMapper;

    public AwsS3Service(IntegrationRepository integrationRepository,
            IntegrationSecretRepository integrationSecretRepository,
            ObjectMapper objectMapper) {
        this.integrationRepository = integrationRepository;
        this.integrationSecretRepository = integrationSecretRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<S3BucketInfo> getS3Buckets(UUID projectId) {
        AwsSecretPayload credentials = getAwsCredentials(projectId);

        try (S3Client s3Client = createS3Client(credentials)) {
            ListBucketsResponse response = s3Client.listBuckets();

            List<S3BucketInfo> buckets = new ArrayList<>();
            for (Bucket bucket : response.buckets()) {
                // Get object count and size for each bucket
                long objectCount = 0;
                long totalSize = 0;

                try {
                    ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                            .bucket(bucket.name())
                            .maxKeys(1000)
                            .build();

                    ListObjectsV2Response listResponse = s3Client.listObjectsV2(listRequest);
                    objectCount = listResponse.keyCount();

                    for (S3Object object : listResponse.contents()) {
                        totalSize += object.size();
                    }
                } catch (Exception e) {
                    // If we can't access bucket details, just skip
                    System.err.println("Could not get details for bucket: " + bucket.name());
                }

                S3BucketInfo info = new S3BucketInfo(
                        bucket.name(),
                        bucket.creationDate() != null ? bucket.creationDate().toString() : "",
                        objectCount,
                        totalSize,
                        credentials.region());
                buckets.add(info);
            }

            return buckets;
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch S3 buckets: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public S3Metrics getS3Metrics(UUID projectId) {
        List<S3BucketInfo> buckets = getS3Buckets(projectId);

        int totalBuckets = buckets.size();
        long totalObjects = 0;
        long totalStorageBytes = 0;

        for (S3BucketInfo bucket : buckets) {
            totalObjects += bucket.objectCount();
            totalStorageBytes += bucket.sizeBytes();
        }

        // Convert bytes to GB
        double totalStorageGB = totalStorageBytes / (1024.0 * 1024.0 * 1024.0);

        return new S3Metrics(
                totalBuckets,
                totalObjects,
                totalStorageGB,
                buckets);
    }

    private AwsSecretPayload getAwsCredentials(UUID projectId) {
        // Find AWS integration for this project
        Integration integration = integrationRepository
                .findByProjectIdAndIntegrationType(projectId, IntegrationType.AWS)
                .stream()
                .filter(i -> i.getIntegrationStatus() == IntegrationStatus.CONNECTED)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No connected AWS integration found for project"));

        // Get secret
        IntegrationSecret secret = integrationSecretRepository
                .findByIntegrationId(integration.getId())
                .orElseThrow(() -> new IllegalArgumentException("AWS credentials not found"));

        try {
            return objectMapper.readValue(secret.getSecretJson(), AwsSecretPayload.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse AWS credentials", e);
        }
    }

    private S3Client createS3Client(AwsSecretPayload credentials) {
        AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(
                credentials.accessKeyId(),
                credentials.secretAccessKey());

        return S3Client.builder()
                .region(Region.of(credentials.region()))
                .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
                .build();
    }

    public record S3BucketInfo(
            String name,
            String creationDate,
            long objectCount,
            long sizeBytes,
            String region) {
    }

    public record S3Metrics(
            int totalBuckets,
            long totalObjects,
            double totalStorageGB,
            List<S3BucketInfo> buckets) {
    }
}
