package com.duelistic.system;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.duelistic.ui.ConsoleUi;

/**
 * Periodically removes expired bans from the SQL backing store.
 */
public class BanCleanupService {
    private static final long DEFAULT_INTERVAL_MS = 60_000;
    private final SqlConfig sqlConfig;
    private final ScheduledExecutorService executor;
    private final long intervalMs;
    private boolean initialized;

    /**
     * Creates the cleanup service with the default interval.
     */
    public BanCleanupService(SqlConfig sqlConfig) {
        this(sqlConfig, DEFAULT_INTERVAL_MS);
    }

    /**
     * Creates the cleanup service with a custom interval.
     *
     * @param intervalMs interval between cleanup runs.
     */
    public BanCleanupService(SqlConfig sqlConfig, long intervalMs) {
        this.sqlConfig = sqlConfig;
        this.intervalMs = intervalMs;
        this.executor = Executors.newSingleThreadScheduledExecutor();
    }

    /**
     * Starts scheduled cleanup if SQL is configured and enabled.
     */
    public void start() {
        if (sqlConfig == null || !sqlConfig.isEnabled()) {
            ConsoleUi.info("SQL ban cleanup disabled (system/sql.yml).");
            return;
        }
        if (!isConfigValid()) {
            ConsoleUi.warn("SQL ban cleanup enabled but config is incomplete.");
            return;
        }
        if (sqlConfig.getDriver() != null) {
            try {
                Class.forName(sqlConfig.getDriver());
            } catch (ClassNotFoundException e) {
                ConsoleUi.error("SQL driver not found: " + e.getMessage());
                return;
            }
        }
        long initialDelay = Math.min(5_000, intervalMs);
        executor.scheduleAtFixedRate(this::cleanupOnce, initialDelay, intervalMs, TimeUnit.MILLISECONDS);
        ConsoleUi.info("SQL ban cleanup started (every " + (intervalMs / 1000) + "s).");
    }

    /**
     * Stops the scheduled cleanup task immediately.
     */
    public void stop() {
        executor.shutdownNow();
    }

    /**
     * Ensures required SQL config values are present.
     */
    private boolean isConfigValid() {
        return sqlConfig.getUrl() != null && !sqlConfig.getUrl().trim().isEmpty();
    }

    /**
     * Executes a single cleanup cycle.
     */
    private void cleanupOnce() {
        try (Connection connection = openConnection()) {
            ensureTable(connection);
            deleteExpiredBans(connection);
        } catch (Exception e) {
            ConsoleUi.error("SQL ban cleanup failed: " + e.getMessage());
        }
    }

    /**
     * Opens a JDBC connection using the configured credentials.
     */
    private Connection openConnection() throws SQLException {
        String username = sqlConfig.getUsername();
        if (username != null && !username.trim().isEmpty()) {
            String password = sqlConfig.getPassword();
            if (password == null) {
                password = "";
            }
            return DriverManager.getConnection(sqlConfig.getUrl(), username, password);
        }
        return DriverManager.getConnection(sqlConfig.getUrl());
    }

    /**
     * Creates the ban table if it does not already exist.
     */
    private void ensureTable(Connection connection) throws SQLException {
        if (initialized) {
            return;
        }
        String createSql = "CREATE TABLE IF NOT EXISTS active_bans ("
            + "id INT UNSIGNED NOT NULL AUTO_INCREMENT,"
            + "username VARCHAR(64) NOT NULL,"
            + "reason VARCHAR(255) NOT NULL,"
            + "started_at DATETIME NOT NULL,"
            + "length_minutes INT UNSIGNED NOT NULL DEFAULT 0,"
            + "PRIMARY KEY (id),"
            + "INDEX idx_started_at (started_at),"
            + "INDEX idx_username (username)"
            + ")";
        try (Statement statement = connection.createStatement()) {
            statement.execute(createSql);
        }
        initialized = true;
    }

    /**
     * Removes any ban rows whose duration has elapsed.
     */
    private void deleteExpiredBans(Connection connection) throws SQLException {
        String deleteSql = "DELETE FROM active_bans "
            + "WHERE length_minutes > 0 "
            + "AND started_at + INTERVAL length_minutes MINUTE <= NOW()";
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(deleteSql);
        }
    }
}
