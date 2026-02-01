package com.threadspace.backend.integration.vercel;

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

    public record ProjectSummary(
            String id,
            String name,
            String framework,
            String repo,
            List<VercelDomain> domains,
            List<VercelEnvVar> envs) {
    }
}
