package com.threadspace.backend.integration.core;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IntegrationSecretRepository extends JpaRepository<IntegrationSecret, UUID> {

    Optional<IntegrationSecret> findByIntegrationId(UUID integrationId);

    /**
     * Deletes an integration's secret row <em>and</em> the credentials it points to.
     *
     * {@code secret_json} is an {@code @Lob}, stored as an oid pointing into
     * pg_largeobject. Deleting only the row leaves the credentials readable in
     * the database. This deletes the row and unlinks its large object in one
     * statement, without loading the entity - loading would try to read the
     * large object, which fails once it's been unlinked.
     */
    @Query(value = """
            WITH removed AS (
                DELETE FROM "integrationSecrets" WHERE integration_id = :integrationId RETURNING secret_json
            )
            SELECT lo_unlink(secret_json) FROM removed
            """, nativeQuery = true)
    List<Integer> deleteWithCredentials(@Param("integrationId") UUID integrationId);
}
