package com.threadspace.backend.integration.vercel;

import com.fasterxml.jackson.annotation.JsonAlias;
import java.util.List;

public class VercelDtos {
    public record VercelProject(
            String id,
            String name,
            VercelGit git,
            String framework,
            String accountId) {
    }

    public record VercelGit(String repository, String branch) {
    }

    public record VercelDomain(String name, boolean verified, boolean primary) {
    }

    public record VercelDomainsResponse(List<VercelDomain> domains) {
    }

    public record VercelEnvVar(String key, List<String> target) {
    }

    public record VercelEnvResponse(List<VercelEnvVar> envs) {
    }

    public record VercelDeployment(
            @JsonAlias("uid") String id,
            String url,
            String state,
            String target,
            Long createdAt) {
    }

    public record VercelDeploymentsResponse(List<VercelDeployment> deployments) {
    }

    public record ProjectSummary(
            String id,
            String name,
            String framework,
            String repo,
            List<VercelDomain> domains,
            List<VercelEnvVar> envs,
            List<VercelDeployment> deployments) {
    }
}
