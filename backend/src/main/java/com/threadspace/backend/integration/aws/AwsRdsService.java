package com.threadspace.backend.integration.aws;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.Datapoint;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.GetMetricStatisticsRequest;
import software.amazon.awssdk.services.cloudwatch.model.GetMetricStatisticsResponse;
import software.amazon.awssdk.services.cloudwatch.model.Statistic;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.model.DBInstance;
import software.amazon.awssdk.services.rds.model.DescribeDbInstancesResponse;

@Service
public class AwsRdsService {

    private final IntegrationRepository integrationRepository;
    private final IntegrationSecretRepository integrationSecretRepository;
    private final ObjectMapper objectMapper;

    public AwsRdsService(IntegrationRepository integrationRepository,
            IntegrationSecretRepository integrationSecretRepository,
            ObjectMapper objectMapper) {
        this.integrationRepository = integrationRepository;
        this.integrationSecretRepository = integrationSecretRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<RdsInstanceInfo> getRdsInstances(UUID projectId) {
        AwsSecretPayload credentials = getAwsCredentials(projectId);

        try (RdsClient rdsClient = createRdsClient(credentials)) {
            DescribeDbInstancesResponse response = rdsClient.describeDBInstances();

            List<RdsInstanceInfo> instances = new ArrayList<>();
            for (DBInstance dbInstance : response.dbInstances()) {
                RdsInstanceInfo info = new RdsInstanceInfo(
                        dbInstance.dbInstanceIdentifier(),
                        dbInstance.dbInstanceClass(),
                        dbInstance.engine() + " " + dbInstance.engineVersion(),
                        dbInstance.dbInstanceStatus(),
                        dbInstance.allocatedStorage(),
                        dbInstance.multiAZ(),
                        dbInstance.availabilityZone());
                instances.add(info);
            }

            return instances;
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch RDS instances: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public RdsMetrics getRdsMetrics(UUID projectId) {
        AwsSecretPayload credentials = getAwsCredentials(projectId);

        try (RdsClient rdsClient = createRdsClient(credentials);
                CloudWatchClient cloudWatchClient = createCloudWatchClient(credentials)) {

            DescribeDbInstancesResponse rdsResponse = rdsClient.describeDBInstances();
            List<DBInstance> instances = rdsResponse.dbInstances();

            if (instances.isEmpty()) {
                return new RdsMetrics(0, 0, 0, 0.0, List.of(), List.of(), List.of(), List.of());
            }

            // Calculate aggregate metrics
            int totalDatabases = instances.size();
            int totalConnections = 0;
            long totalStorage = 0;

            for (DBInstance instance : instances) {
                totalStorage += instance.allocatedStorage();
            }

            // Fetch CloudWatch metrics for the first instance (as example)
            DBInstance firstInstance = instances.get(0);
            String dbInstanceId = firstInstance.dbInstanceIdentifier();

            List<MetricDataPoint> cpuData = getMetric(cloudWatchClient, dbInstanceId, "CPUUtilization", 24);
            List<MetricDataPoint> connectionsData = getMetric(cloudWatchClient, dbInstanceId, "DatabaseConnections",
                    24);
            List<MetricDataPoint> readIopsData = getMetric(cloudWatchClient, dbInstanceId, "ReadIOPS", 24);
            List<MetricDataPoint> writeIopsData = getMetric(cloudWatchClient, dbInstanceId, "WriteIOPS", 24);

            // Calculate average CPU
            double avgCpu = cpuData.stream()
                    .mapToDouble(MetricDataPoint::value)
                    .average()
                    .orElse(0.0);

            // Get latest connections count
            if (!connectionsData.isEmpty()) {
                totalConnections = (int) connectionsData.get(connectionsData.size() - 1).value();
            }

            return new RdsMetrics(
                    totalDatabases,
                    totalConnections,
                    (int) totalStorage,
                    avgCpu,
                    cpuData,
                    connectionsData,
                    readIopsData,
                    writeIopsData);

        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch RDS metrics: " + e.getMessage(), e);
        }
    }

    private List<MetricDataPoint> getMetric(CloudWatchClient cloudWatchClient, String dbInstanceId,
            String metricName, int hours) {
        Instant endTime = Instant.now();
        Instant startTime = endTime.minus(hours, ChronoUnit.HOURS);

        GetMetricStatisticsRequest request = GetMetricStatisticsRequest.builder()
                .namespace("AWS/RDS")
                .metricName(metricName)
                .dimensions(Dimension.builder()
                        .name("DBInstanceIdentifier")
                        .value(dbInstanceId)
                        .build())
                .startTime(startTime)
                .endTime(endTime)
                .period(3600) // 1 hour
                .statistics(Statistic.AVERAGE)
                .build();

        GetMetricStatisticsResponse response = cloudWatchClient.getMetricStatistics(request);

        List<MetricDataPoint> dataPoints = new ArrayList<>();
        for (Datapoint dp : response.datapoints()) {
            dataPoints.add(new MetricDataPoint(
                    dp.timestamp().toString(),
                    dp.average()));
        }

        // Sort by timestamp
        dataPoints.sort((a, b) -> a.timestamp().compareTo(b.timestamp()));

        return dataPoints;
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

    private RdsClient createRdsClient(AwsSecretPayload credentials) {
        AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(
                credentials.accessKeyId(),
                credentials.secretAccessKey());

        return RdsClient.builder()
                .region(Region.of(credentials.region()))
                .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
                .build();
    }

    private CloudWatchClient createCloudWatchClient(AwsSecretPayload credentials) {
        AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(
                credentials.accessKeyId(),
                credentials.secretAccessKey());

        return CloudWatchClient.builder()
                .region(Region.of(credentials.region()))
                .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
                .build();
    }

    public record RdsInstanceInfo(
            String instanceId,
            String instanceClass,
            String engine,
            String status,
            Integer storageGB,
            Boolean multiAz,
            String availabilityZone) {
    }

    public record RdsMetrics(
            int totalDatabases,
            int activeConnections,
            int storageUsedGB,
            double avgCpuUtilization,
            List<MetricDataPoint> cpuData,
            List<MetricDataPoint> connectionsData,
            List<MetricDataPoint> readIopsData,
            List<MetricDataPoint> writeIopsData) {
    }

    public record MetricDataPoint(String timestamp, double value) {
    }
}
