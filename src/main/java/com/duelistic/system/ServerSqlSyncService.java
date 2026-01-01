package com.duelistic.system;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.duelistic.ui.ConsoleUi;

/**
 * Periodically syncs active server snapshots into SQL.
 */
public class ServerSqlSyncService {
    private static final long DEFAULT_INTERVAL_MS = 10_000;
    private final ServerStatusService statusService;
    private final SqlConfig sqlConfig;
    private final ScheduledExecutorService executor;
    private final long intervalMs;
    private boolean initialized;

    /**
     * Creates a sync service with the default interval.
     */
    public ServerSqlSyncService(ServerStatusService statusService, SqlConfig sqlConfig) {
        this(statusService, sqlConfig, DEFAULT_INTERVAL_MS);
    }

    /**
     * Creates a sync service with a custom interval.
     *
     * @param intervalMs interval between syncs.
     */
    public ServerSqlSyncService(ServerStatusService statusService, SqlConfig sqlConfig, long intervalMs) {
        this.statusService = statusService;
        this.sqlConfig = sqlConfig;
        this.intervalMs = intervalMs;
        this.executor = Executors.newSingleThreadScheduledExecutor();
    }

    /**
     * Starts scheduled syncs if SQL is configured.
     */
    public void start() {
        if (sqlConfig == null || !sqlConfig.isEnabled()) {
            ConsoleUi.info("SQL server sync disabled (system/sql.yml).");
            return;
        }
        if (!isConfigValid()) {
            ConsoleUi.warn("SQL server sync enabled but config is incomplete.");
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
        executor.scheduleAtFixedRate(this::syncOnce, initialDelay, intervalMs, TimeUnit.MILLISECONDS);
        ConsoleUi.info("SQL server sync started (every " + (intervalMs / 1000) + "s).");
    }

    /**
     * Stops syncs and clears the servers table.
     */
    public void stop() {
        executor.shutdownNow();
        clearServers();
    }

    /**
     * Validates required SQL configuration.
     */
    private boolean isConfigValid() {
        return sqlConfig.getUrl() != null && !sqlConfig.getUrl().trim().isEmpty();
    }

    /**
     * Syncs a single snapshot of server status to SQL.
     */
    private void syncOnce() {
        try {
            List<ServerStatus> statuses = statusService.listStatuses();
            try (Connection connection = openConnection()) {
                ensureTable(connection);
                replaceSnapshot(connection, statuses);
            }
        } catch (Exception e) {
            ConsoleUi.error("SQL server sync failed: " + e.getMessage());
        }
    }

    /**
     * Clears the servers table on shutdown.
     */
    private void clearServers() {
        if (sqlConfig == null || !sqlConfig.isEnabled() || !isConfigValid()) {
            return;
        }
        try (Connection connection = openConnection()) {
            ensureTable(connection);
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("DELETE FROM servers");
            }
        } catch (Exception e) {
            ConsoleUi.error("SQL server clear failed: " + e.getMessage());
        }
    }

    /**
     * Opens a JDBC connection using configured credentials.
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
     * Creates the servers table if it does not exist.
     */
    private void ensureTable(Connection connection) throws SQLException {
        if (initialized) {
            return;
        }
        String createSql = "CREATE TABLE IF NOT EXISTS servers ("
            + "id INT UNSIGNED NOT NULL AUTO_INCREMENT,"
            + "server_name VARCHAR(128) NOT NULL,"
            + "template VARCHAR(64) NOT NULL,"
            + "is_online TINYINT(1) NOT NULL DEFAULT 0,"
            + "online_players INT UNSIGNED NOT NULL DEFAULT 0,"
            + "max_players INT UNSIGNED NOT NULL DEFAULT 0,"
            + "started_at DATETIME NULL,"
            + "PRIMARY KEY (id),"
            + "INDEX idx_template (template),"
            + "INDEX idx_started_at (started_at)"
            + ")";
        try (Statement statement = connection.createStatement()) {
            statement.execute(createSql);
        }
        initialized = true;
    }

    /**
     * Replaces the current servers snapshot with a fresh list.
     */
    private void replaceSnapshot(Connection connection, List<ServerStatus> statuses) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("DELETE FROM servers");
        }
        if (statuses.isEmpty()) {
            return;
        }
        String insertSql = "INSERT INTO servers "
            + "(server_name, template, is_online, online_players, max_players, started_at) "
            + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(insertSql)) {
            for (ServerStatus status : statuses) {
                statement.setString(1, status.getName());
                statement.setString(2, status.getTemplate());
                statement.setInt(3, status.isOnline() ? 1 : 0);
                statement.setInt(4, Math.max(0, status.getCurrentPlayers()));
                statement.setInt(5, Math.max(0, status.getMaxPlayers()));
                if (status.getStartedAt() != null) {
                    statement.setTimestamp(6, java.sql.Timestamp.from(status.getStartedAt()));
                } else {
                    statement.setNull(6, Types.TIMESTAMP);
                }
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }
}
