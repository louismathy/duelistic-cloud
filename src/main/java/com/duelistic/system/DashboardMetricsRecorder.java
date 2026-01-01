package com.duelistic.system;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.duelistic.ui.ConsoleUi;

/**
 * Periodically writes aggregate dashboard metrics into SQL.
 */
public class DashboardMetricsRecorder {
    private static final long DEFAULT_INTERVAL_MS = 15_000;
    private final ServerStatusService statusService;
    private final SqlConfig sqlConfig;
    private final ScheduledExecutorService executor;
    private final long intervalMs;
    private boolean initialized;

    /**
     * Creates a recorder with the default interval.
     */
    public DashboardMetricsRecorder(ServerStatusService statusService, SqlConfig sqlConfig) {
        this(statusService, sqlConfig, DEFAULT_INTERVAL_MS);
    }

    /**
     * Creates a recorder with a custom interval.
     *
     * @param intervalMs interval between snapshots.
     */
    public DashboardMetricsRecorder(ServerStatusService statusService, SqlConfig sqlConfig, long intervalMs) {
        this.statusService = statusService;
        this.sqlConfig = sqlConfig;
        this.intervalMs = intervalMs;
        this.executor = Executors.newSingleThreadScheduledExecutor();
    }

    /**
     * Starts scheduled metric writes if SQL is configured.
     */
    public void start() {
        if (sqlConfig == null || !sqlConfig.isEnabled()) {
            ConsoleUi.info("SQL dashboard metrics disabled (system/sql.yml).");
            return;
        }
        if (!isConfigValid()) {
            ConsoleUi.warn("SQL dashboard metrics enabled but config is incomplete.");
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
        executor.scheduleAtFixedRate(this::recordOnce, initialDelay, intervalMs, TimeUnit.MILLISECONDS);
        ConsoleUi.info("SQL dashboard metrics recorder started (every " + (intervalMs / 1000) + "s).");
    }

    /**
     * Stops scheduled writes and clears dashboard metrics.
     */
    public void stop() {
        executor.shutdownNow();
        resetDashboard();
    }

    /**
     * Validates required SQL configuration.
     */
    private boolean isConfigValid() {
        return sqlConfig.getUrl() != null && !sqlConfig.getUrl().trim().isEmpty();
    }

    /**
     * Computes and writes a single metrics snapshot.
     */
    private void recordOnce() {
        try {
            int activeServers = computeActiveServers();
            int onlinePlayers = computeOnlinePlayers();
            int openReports = computeOpenReports();
            try (Connection connection = openConnection()) {
                ensureTable(connection);
                insertSnapshot(connection, activeServers, onlinePlayers, openReports);
            }
        } catch (Exception e) {
            ConsoleUi.error("SQL dashboard metrics write failed: " + e.getMessage());
        }
    }

    /**
     * Computes the number of active servers from status snapshots.
     */
    private int computeActiveServers() throws Exception {
        List<ServerStatus> statuses = statusService.listStatuses();
        return statuses.size();
    }

    /**
     * Computes total online players from status snapshots.
     */
    private int computeOnlinePlayers() throws Exception {
        List<ServerStatus> statuses = statusService.listStatuses();
        int total = 0;
        for (ServerStatus status : statuses) {
            total += Math.max(0, status.getCurrentPlayers());
        }
        return total;
    }

    /**
     * Queries SQL for total open reports.
     */
    private int computeOpenReports() {
        try (Connection connection = openConnection()) {
            ensureReportsTable(connection);
            return countReports(connection);
        } catch (Exception e) {
            ConsoleUi.error("SQL report count failed: " + e.getMessage());
            return 0;
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
     * Creates the dashboard metrics table if needed.
     */
    private void ensureTable(Connection connection) throws SQLException {
        if (initialized) {
            return;
        }
        String createSql = "CREATE TABLE IF NOT EXISTS dashboard_metrics ("
            + "active_servers INT UNSIGNED NOT NULL DEFAULT 0,"
            + "online_players INT UNSIGNED NOT NULL DEFAULT 0,"
            + "open_reports INT UNSIGNED NOT NULL DEFAULT 0"
            + ")";
        try (Statement statement = connection.createStatement()) {
            statement.execute(createSql);
        }
        initialized = true;
    }

    /**
     * Ensures the reports table exists for counting.
     */
    private void ensureReportsTable(Connection connection) throws SQLException {
        String createSql = "CREATE TABLE IF NOT EXISTS player_reports ("
            + "id INT UNSIGNED NOT NULL AUTO_INCREMENT,"
            + "reported_player VARCHAR(64) NOT NULL,"
            + "reporter_player VARCHAR(64) NOT NULL,"
            + "reason VARCHAR(255) NOT NULL,"
            + "location VARCHAR(255) NOT NULL,"
            + "PRIMARY KEY (id),"
            + "INDEX idx_reported_player (reported_player),"
            + "INDEX idx_reporter_player (reporter_player)"
            + ")";
        try (Statement statement = connection.createStatement()) {
            statement.execute(createSql);
        }
    }

    /**
     * Counts report rows in the reports table.
     */
    private int countReports(Connection connection) throws SQLException {
        String query = "SELECT COUNT(*) AS total FROM player_reports";
        try (Statement statement = connection.createStatement();
             java.sql.ResultSet resultSet = statement.executeQuery(query)) {
            if (resultSet.next()) {
                return resultSet.getInt("total");
            }
        }
        return 0;
    }

    /**
     * Replaces the current dashboard snapshot row.
     */
    private void insertSnapshot(Connection connection, int activeServers, int onlinePlayers, int openReports) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("DELETE FROM dashboard_metrics");
        }
        String insertSql = "INSERT INTO dashboard_metrics (active_servers, online_players, open_reports) VALUES (?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(insertSql)) {
            statement.setInt(1, activeServers);
            statement.setInt(2, onlinePlayers);
            statement.setInt(3, openReports);
            statement.executeUpdate();
        }
    }

    /**
     * Resets the dashboard snapshot to zeros when shutting down.
     */
    private void resetDashboard() {
        if (sqlConfig == null || !sqlConfig.isEnabled() || !isConfigValid()) {
            return;
        }
        try (Connection connection = openConnection()) {
            ensureTable(connection);
            insertSnapshot(connection, 0, 0, computeOpenReports());
        } catch (Exception e) {
            ConsoleUi.error("SQL dashboard reset failed: " + e.getMessage());
        }
    }
}
