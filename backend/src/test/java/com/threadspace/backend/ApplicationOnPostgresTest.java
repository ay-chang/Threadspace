package com.threadspace.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.threadspace.backend.integration.core.EnumCheckConstraintSync;
import com.threadspace.backend.integration.core.Integration;
import com.threadspace.backend.integration.core.IntegrationCredentials;
import com.threadspace.backend.integration.core.IntegrationRepository;
import com.threadspace.backend.integration.core.IntegrationSecret;
import com.threadspace.backend.integration.core.IntegrationSecretRepository;
import com.threadspace.backend.integration.core.IntegrationService;
import com.threadspace.backend.integration.core.IntegrationStatus;
import com.threadspace.backend.integration.core.IntegrationType;
import com.threadspace.backend.integration.stripe.StripeSecretPayload;
import com.threadspace.backend.integration.testsupport.LocalPostgres;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Boots the whole application against a real, throwaway Postgres. Unit and
 * controller-slice tests mock the repositories, so they can't see problems in
 * bean wiring or in how Postgres stores the secret column - both of which
 * broke the app once. Skipped where Postgres isn't installed.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class ApplicationOnPostgresTest {

    private static LocalPostgres postgres;

    @Autowired
    private IntegrationRepository integrations;
    @Autowired
    private IntegrationSecretRepository secrets;
    @Autowired
    private IntegrationCredentials credentials;
    @Autowired
    private IntegrationService integrationService;
    @Autowired
    private EnumCheckConstraintSync constraintSync;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private ObjectMapper mapper;

    @BeforeAll
    static void requirePostgres() {
        assumeTrue(LocalPostgres.binDir().isPresent(), "Postgres server binaries not installed");
    }

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) throws Exception {
        if (postgres == null) {
            postgres = LocalPostgres.start();
            LocalPostgres started = postgres;
            // Outlive the (cached) Spring context; stop when the test JVM exits.
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    started.close();
                } catch (Exception ignored) {
                }
            }));
        }
        registry.add("DB_URL", postgres::jdbcUrl);
        registry.add("DB_USERNAME", () -> "postgres");
        registry.add("DB_PASSWORD", () -> "");
        registry.add("spring.datasource.url", postgres::jdbcUrl);
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "");
        registry.add("INTERNAL_SYNC_TOKEN", () -> "test-token");
    }

    @Test
    void theApplicationContextStarts() {
        // Reaching this point means every bean could be constructed - the
        // constraint sync once couldn't, which stopped the app from booting.
        assertThat(constraintSync).isNotNull();
    }

    @Test
    void storedCredentialsCanBeReadOutsideACallersTransaction() throws Exception {
        UUID project = UUID.randomUUID();
        saveConnected(project, IntegrationType.STRIPE, new StripeSecretPayload("rk_live_onPostgres1234", true));

        // Metrics services call this with no transaction of their own. The
        // secret is a Postgres large object, readable only inside one.
        StripeSecretPayload loaded = credentials.loadConnected(
                project, IntegrationType.STRIPE, StripeSecretPayload.class, "Stripe");

        assertThat(loaded.apiKey()).isEqualTo("rk_live_onPostgres1234");
    }

    @Test
    void disconnectRemovesTheStoredCredentialsCompletely() throws Exception {
        UUID project = UUID.randomUUID();
        Integration integration = saveConnected(project, IntegrationType.GOOGLE_ANALYTICS,
                new StripeSecretPayload("secret-that-must-not-survive", true));
        Long oid = jdbc.queryForObject(
                "SELECT secret_json FROM \"integrationSecrets\" WHERE integration_id = ?", Long.class, integration.getId());
        assertThat(largeObjectExists(oid)).isTrue();

        integrationService.deleteIntegration(project, integration.getId());

        assertThat(integrations.findById(integration.getId())).isEmpty();
        assertThat(secrets.findByIntegrationId(integration.getId())).isEmpty();
        // The row is gone; the credentials themselves must be too.
        assertThat(largeObjectExists(oid)).isFalse();
    }

    @Test
    void aFreshDatabaseAcceptsEveryIntegrationType() throws Exception {
        for (IntegrationType type : IntegrationType.values()) {
            saveConnected(UUID.randomUUID(), type, new StripeSecretPayload("k", true));
        }
    }

    private Integration saveConnected(UUID project, IntegrationType type, Object payload) throws Exception {
        Integration integration = new Integration();
        integration.setProjectId(project);
        integration.setIntegrationType(type);
        integration.setIntegrationStatus(IntegrationStatus.CONNECTED);
        integration.setDisplayName(type.name());
        integration = integrations.save(integration);

        IntegrationSecret secret = new IntegrationSecret();
        secret.setIntegrationId(integration.getId());
        secret.setProvider(type);
        secret.setSecretJson(mapper.writeValueAsString(payload));
        secrets.save(secret);
        return integration;
    }

    private boolean largeObjectExists(Long oid) {
        Integer count = jdbc.queryForObject("SELECT count(*) FROM pg_largeobject_metadata WHERE oid = ?", Integer.class, oid);
        return count != null && count > 0;
    }
}
