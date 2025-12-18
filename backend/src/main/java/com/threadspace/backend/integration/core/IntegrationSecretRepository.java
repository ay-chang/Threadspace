package com.threadspace.backend.integration.core;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface IntegrationSecretRepository extends JpaRepository<IntegrationSecret, UUID> {

    Optional<IntegrationSecret> findByIntegrationId(UUID integrationId);
}
