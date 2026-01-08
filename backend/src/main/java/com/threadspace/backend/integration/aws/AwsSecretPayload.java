package com.threadspace.backend.integration.aws;

public record AwsSecretPayload(
        String accessKeyId,
        String secretAccessKey,
        String region,
        String roleArn) {
}
