package com.threadspace.backend.integration.core;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface IntegrationRepository extends JpaRepository<Integration, UUID> {

    List<Integration> findByProjectId(UUID projectId);
}
