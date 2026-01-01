package com.duelistic.system;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.duelistic.ui.ConsoleUi;

/**
 * Syncs template names into SQL for external dashboards.
 */
public class TemplateSqlSyncService {
    private static final long DEFAULT_INTERVAL_MS = 15_000;
    private final CloudDirectories directories;
    private final SqlConfig sqlConfig;
    private final ScheduledExecutorService executor;
    private final long intervalMs;
    private boolean initialized;

    /**
     * Creates a sync service with the default interval.
     */
    public TemplateSqlSyncService(CloudDirectories directories, SqlConfig sqlConfig) {
        this(directories, sqlConfig, DEFAULT_INTERVAL_MS);
    }

    /**
     * Creates a sync service with a custom interval.
     *
     * @param intervalMs interval between syncs.
     */
    public TemplateSqlSyncService(CloudDirectories directories, SqlConfig sqlConfig, long intervalMs) {
        this.directories = directories;
        this.sqlConfig = sqlConfig;
        this.intervalMs = intervalMs;
        this.executor = Executors.newSingleThreadScheduledExecutor();
    }

    /**
     * Starts scheduled template sync if SQL is configured.
     */
    public void start() {
        if (sqlConfig == null || !sqlConfig.isEnabled()) {
            ConsoleUi.info("SQL template sync disabled (system/sql.yml).");
            return;
        }
        if (!isConfigValid()) {
            ConsoleUi.warn("SQL template sync enabled but config is incomplete.");
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
        ConsoleUi.info("SQL template sync started (every " + (intervalMs / 1000) + "s).");
    }

    /**
     * Stops scheduled syncs immediately.
     */
    public void stop() {
        executor.shutdownNow();
    }

    /**
     * Validates required SQL configuration.
     */
    private boolean isConfigValid() {
        return sqlConfig.getUrl() != null && !sqlConfig.getUrl().trim().isEmpty();
    }

    /**
     * Syncs the current template list into SQL.
     */
    private void syncOnce() {
        try {
            List<String> templates = directories.listTemplates();
            try (Connection connection = openConnection()) {
                ensureTable(connection);
                syncTemplates(connection, templates);
            }
        } catch (Exception e) {
            ConsoleUi.error("SQL template sync failed: " + e.getMessage());
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
     * Creates the templates table if it does not exist.
     */
    private void ensureTable(Connection connection) throws SQLException {
        if (initialized) {
            return;
        }
        String createSql = "CREATE TABLE IF NOT EXISTS server_templates ("
            + "id INT UNSIGNED NOT NULL AUTO_INCREMENT,"
            + "name VARCHAR(64) NOT NULL,"
            + "PRIMARY KEY (id),"
            + "UNIQUE KEY uniq_template_name (name)"
            + ")";
        try (Statement statement = connection.createStatement()) {
            statement.execute(createSql);
        }
        initialized = true;
    }

    /**
     * Inserts missing templates and removes stale ones.
     */
    private void syncTemplates(Connection connection, List<String> templates) throws SQLException {
        Set<String> existing = loadExistingTemplates(connection);
        Set<String> desired = new HashSet<>(templates);
        if (!desired.isEmpty()) {
            insertMissing(connection, desired, existing);
        }
        if (!existing.isEmpty()) {
            deleteMissing(connection, existing, desired);
        }
    }

    /**
     * Loads existing template names from SQL.
     */
    private Set<String> loadExistingTemplates(Connection connection) throws SQLException {
        Set<String> existing = new HashSet<>();
        String query = "SELECT name FROM server_templates";
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {
            while (resultSet.next()) {
                existing.add(resultSet.getString("name"));
            }
        }
        return existing;
    }

    /**
     * Inserts any templates that are not already present.
     */
    private void insertMissing(Connection connection, Set<String> desired, Set<String> existing) throws SQLException {
        String insertSql = "INSERT INTO server_templates (name) VALUES (?)";
        try (PreparedStatement statement = connection.prepareStatement(insertSql)) {
            for (String name : desired) {
                if (existing.contains(name)) {
                    continue;
                }
                statement.setString(1, name);
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    /**
     * Removes templates that no longer exist on disk.
     */
    private void deleteMissing(Connection connection, Set<String> existing, Set<String> desired) throws SQLException {
        String deleteSql = "DELETE FROM server_templates WHERE name = ?";
        try (PreparedStatement statement = connection.prepareStatement(deleteSql)) {
            for (String name : existing) {
                if (desired.contains(name)) {
                    continue;
                }
                statement.setString(1, name);
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }
}
