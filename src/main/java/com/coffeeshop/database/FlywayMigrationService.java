package com.coffeeshop.database;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfoService;
import org.flywaydb.core.api.output.MigrateResult;

import javax.sql.DataSource;
import java.util.Objects;

/** Provides explicit, never-automatic validation and execution of versioned migrations. */
public final class FlywayMigrationService {

    private static final String MIGRATION_LOCATION = "classpath:db/migration";

    private final Flyway flyway;

    public FlywayMigrationService(DataSource dataSource) {
        Objects.requireNonNull(dataSource);
        this.flyway = Flyway.configure()
                .dataSource(dataSource)
                .defaultSchema("dbo")
                .locations(MIGRATION_LOCATION)
                .baselineOnMigrate(false)
                .cleanDisabled(true)
                .validateMigrationNaming(true)
                .validateOnMigrate(true)
                .load();
    }

    public void validate() {
        flyway.validate();
    }

    public MigrationInfoService info() {
        return flyway.info();
    }

    /** Runs pending migrations only when explicitly invoked by application orchestration. */
    public MigrateResult migrate() {
        return flyway.migrate();
    }
}
