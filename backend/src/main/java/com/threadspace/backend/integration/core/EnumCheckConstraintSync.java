package com.threadspace.backend.integration.core;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Keeps Postgres CHECK constraints on enum columns in step with the Java enums.
 *
 * Hibernate writes an enum's allowed values into a CHECK constraint when it
 * creates a table, but {@code ddl-auto=update} never revisits it. Adding an
 * enum constant (as GOOGLE_CLOUD once did) then makes every insert of the new
 * value fail on any database whose tables already existed.
 *
 * On startup this compares each constraint with its enum and, only when values
 * are missing, rebuilds it. It only ever adds values - anything the database
 * already allows stays allowed, so existing rows can't become invalid - and it
 * never creates a constraint where there wasn't one.
 */
@Component
public class EnumCheckConstraintSync implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(EnumCheckConstraintSync.class);

    public record EnumColumn(String table, String column, Class<? extends Enum<?>> enumType) {
    }

    /** Every {@code @Enumerated(EnumType.STRING)} column. Add new ones here. */
    static final List<EnumColumn> COLUMNS = List.of(
            new EnumColumn("Integrations", "type", IntegrationType.class),
            new EnumColumn("Integrations", "status", IntegrationStatus.class),
            new EnumColumn("integrationSecrets", "provider", IntegrationType.class));

    private static final Pattern QUOTED_VALUE = Pattern.compile("'((?:[^']|'')*)'");

    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;
    private final List<EnumColumn> columns;

    @Autowired
    public EnumCheckConstraintSync(JdbcTemplate jdbc, PlatformTransactionManager transactionManager) {
        this(jdbc, transactionManager, COLUMNS);
    }

    EnumCheckConstraintSync(JdbcTemplate jdbc, PlatformTransactionManager transactionManager,
            List<EnumColumn> columns) {
        this.jdbc = jdbc;
        this.transactions = new TransactionTemplate(transactionManager);
        this.columns = columns;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!isPostgres()) {
            return;
        }
        for (EnumColumn column : columns) {
            try {
                sync(column);
            } catch (DataAccessException e) {
                // Don't take the app down over this; the affected enum value
                // will fail on insert and the log says why.
                log.error("Could not update the CHECK constraint on {}.{}", column.table(), column.column(), e);
            }
        }
    }

    /** @return the values added, empty if the constraint was already current or absent */
    Set<String> sync(EnumColumn column) {
        Boolean exists = jdbc.queryForObject("SELECT to_regclass(?) IS NOT NULL", Boolean.class,
                quote(column.table()));
        if (!Boolean.TRUE.equals(exists)) {
            return Set.of();
        }

        List<Map<String, Object>> constraints = jdbc.queryForList("""
                SELECT con.conname AS name, pg_get_constraintdef(con.oid) AS definition
                FROM pg_constraint con
                WHERE con.conrelid = to_regclass(?) AND con.contype = 'c'
                """, quote(column.table()));

        Pattern columnReference = Pattern.compile("\\(\"?" + Pattern.quote(column.column()) + "\"?\\)");
        List<String> matchingNames = new ArrayList<>();
        Set<String> allowed = new LinkedHashSet<>();
        for (Map<String, Object> constraint : constraints) {
            String definition = String.valueOf(constraint.get("definition"));
            if (columnReference.matcher(definition).find()) {
                matchingNames.add(String.valueOf(constraint.get("name")));
                Matcher values = QUOTED_VALUE.matcher(definition);
                while (values.find()) {
                    allowed.add(values.group(1).replace("''", "'"));
                }
            }
        }
        if (matchingNames.isEmpty()) {
            return Set.of();
        }

        List<String> required = Arrays.stream(column.enumType().getEnumConstants())
                .map(Enum::name)
                .toList();
        Set<String> missing = new LinkedHashSet<>(required);
        missing.removeAll(allowed);
        if (missing.isEmpty()) {
            return Set.of();
        }

        Set<String> values = new LinkedHashSet<>(required);
        values.addAll(allowed);
        String valueList = values.stream()
                .map(v -> "'" + v.replace("'", "''") + "'")
                .collect(Collectors.joining(", "));
        String constraintName = column.table() + "_" + column.column() + "_check";

        transactions.executeWithoutResult(status -> {
            for (String name : matchingNames) {
                jdbc.execute("ALTER TABLE " + quote(column.table()) + " DROP CONSTRAINT " + quote(name));
            }
            jdbc.execute("ALTER TABLE " + quote(column.table()) + " ADD CONSTRAINT " + quote(constraintName)
                    + " CHECK ((" + quote(column.column()) + ")::text = ANY (ARRAY[" + valueList + "]::text[]))");
        });

        log.info("Allowed {} in {}.{} (CHECK constraint was out of date with {})",
                missing, column.table(), column.column(), column.enumType().getSimpleName());
        return missing;
    }

    private boolean isPostgres() {
        try {
            return Boolean.TRUE.equals(jdbc.execute((ConnectionCallback<Boolean>) (Connection c) ->
                    "PostgreSQL".equalsIgnoreCase(c.getMetaData().getDatabaseProductName())));
        } catch (DataAccessException e) {
            log.warn("Could not determine database type; skipping CHECK constraint sync", e);
            return false;
        }
    }

    private static String quote(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }
}
