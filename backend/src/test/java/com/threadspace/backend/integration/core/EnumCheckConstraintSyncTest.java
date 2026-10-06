package com.threadspace.backend.integration.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.List;

import com.threadspace.backend.integration.core.EnumCheckConstraintSync.EnumColumn;
import com.threadspace.backend.integration.testsupport.LocalPostgres;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/**
 * Runs against a real, throwaway Postgres: the whole point is matching the
 * constraint text Postgres actually produces. Skipped where Postgres isn't installed.
 */
class EnumCheckConstraintSyncTest {

    /** Exactly what the Supabase database held before GOOGLE_CLOUD was added by hand. */
    private static final String STALE_CONSTRAINT = """
            CHECK (((type)::text = ANY ((ARRAY['VERCEL'::character varying, 'APPLE_CONNECT'::character varying,
            'STRIPE'::character varying, 'AWS'::character varying, 'LEGACY_TYPE'::character varying])::text[])))""";

    private static LocalPostgres postgres;
    private static JdbcTemplate jdbc;
    private static EnumCheckConstraintSync sync;

    @BeforeAll
    static void startPostgres() throws Exception {
        assumeTrue(LocalPostgres.binDir().isPresent(), "Postgres server binaries not installed");
        postgres = LocalPostgres.start();
        DriverManagerDataSource dataSource = new DriverManagerDataSource(postgres.jdbcUrl(), "postgres", "");
        jdbc = new JdbcTemplate(dataSource);
        sync = new EnumCheckConstraintSync(jdbc, new DataSourceTransactionManager(dataSource),
                List.of(new EnumColumn("Integrations", "type", IntegrationType.class)));
    }

    @AfterAll
    static void stopPostgres() throws Exception {
        if (postgres != null) {
            postgres.close();
        }
    }

    @BeforeEach
    void freshTable() {
        jdbc.execute("DROP TABLE IF EXISTS \"Integrations\"");
        jdbc.execute("CREATE TABLE \"Integrations\" (id serial PRIMARY KEY, \"type\" varchar(255) NOT NULL, "
                + "CONSTRAINT \"Integrations_type_check\" " + STALE_CONSTRAINT + ")");
    }

    @Test
    void staleConstraintRejectsNewEnumValuesUntilSynced() {
        assertThatThrownBy(() -> insert("GOOGLE_ANALYTICS")).isInstanceOf(DataIntegrityViolationException.class);

        var added = sync.sync(column());

        assertThat(added).containsExactlyInAnyOrder("GOOGLE_CLOUD", "GOOGLE_ANALYTICS");
        insert("GOOGLE_ANALYTICS");
        insert("GOOGLE_CLOUD");
        insert("STRIPE");
    }

    @Test
    void neverRemovesValuesTheDatabaseAlreadyAllowed() {
        insert("LEGACY_TYPE"); // not in the Java enum any more

        sync.sync(column());

        insert("LEGACY_TYPE");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM \"Integrations\" WHERE \"type\" = 'LEGACY_TYPE'",
                Integer.class)).isEqualTo(2);
    }

    @Test
    void isANoOpOnceCurrent() {
        sync.sync(column());
        String before = constraintDefinitions();

        assertThat(sync.sync(column())).isEmpty();
        assertThat(constraintDefinitions()).isEqualTo(before);
    }

    @Test
    void stillRejectsValuesOutsideTheEnum() {
        sync.sync(column());

        assertThatThrownBy(() -> insert("NOT_A_REAL_TYPE")).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void leavesUnconstrainedColumnsAlone() {
        jdbc.execute("ALTER TABLE \"Integrations\" DROP CONSTRAINT \"Integrations_type_check\"");

        assertThat(sync.sync(column())).isEmpty();
        assertThat(constraintDefinitions()).isEmpty();
    }

    @Test
    void skipsTablesThatDontExistYet() {
        jdbc.execute("DROP TABLE \"Integrations\"");

        assertThat(sync.sync(column())).isEmpty();
    }

    @Test
    void acceptsTheConstraintHibernateItselfGenerates() {
        // A fresh database: Hibernate creates the constraint from the current enum.
        jdbc.execute("DROP TABLE \"Integrations\"");
        jdbc.execute("CREATE TABLE \"Integrations\" (id serial PRIMARY KEY, \"type\" varchar(255) NOT NULL "
                + "CHECK (\"type\" IN ('VERCEL','AWS','GOOGLE_CLOUD','APPLE_CONNECT','STRIPE','GOOGLE_ANALYTICS')))");

        assertThat(sync.sync(column())).isEmpty();
    }

    private static EnumColumn column() {
        return new EnumColumn("Integrations", "type", IntegrationType.class);
    }

    private static void insert(String type) {
        jdbc.update("INSERT INTO \"Integrations\" (\"type\") VALUES (?)", type);
    }

    private static String constraintDefinitions() {
        return String.join("|", jdbc.queryForList(
                "SELECT pg_get_constraintdef(oid) FROM pg_constraint WHERE conrelid = to_regclass('\"Integrations\"') "
                        + "AND contype = 'c' ORDER BY conname", String.class));
    }
}
