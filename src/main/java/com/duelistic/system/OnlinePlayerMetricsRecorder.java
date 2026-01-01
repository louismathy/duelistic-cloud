package com.duelistic.system;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.duelistic.ui.ConsoleUi;

/**
 * Writes periodic online player samples to SQL for historical tracking.
 */
public class OnlinePlayerMetricsRecorder {
    private static final long DEFAULT_INTERVAL_MS = 30 * 60_000;
    private final ServerStatusService statusService;
    private final SqlConfig sqlConfig;
    private final ScheduledExecutorService executor;
    private final long intervalMs;
    private boolean initialized;

    /**
     * Creates a recorder with the default interval.
     */
    public OnlinePlayerMetricsRecorder(ServerStatusService statusService, SqlConfig sqlConfig) {
        this(statusService, sqlConfig, DEFAULT_INTERVAL_MS);
    }

    /**
     * Creates a recorder with a custom sample interval.
     *
     * @param intervalMs interval between samples.
     */
    public OnlinePlayerMetricsRecorder(ServerStatusService statusService, SqlConfig sqlConfig, long intervalMs) {
        this.statusService = statusService;
        this.sqlConfig = sqlConfig;
        this.intervalMs = intervalMs;
        this.executor = Executors.newSingleThreadScheduledExecutor();
    }

    /**
     * Starts scheduled sampling if SQL is configured.
     */
    public void start() {
        if (sqlConfig == null || !sqlConfig.isEnabled()) {
            ConsoleUi.info("SQL metrics disabled (system/sql.yml).");
            return;
        }
        if (!isConfigValid()) {
            ConsoleUi.warn("SQL metrics enabled but config is incomplete.");
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
        long initialDelay = computeInitialDelay();
        executor.scheduleAtFixedRate(this::recordOnce, initialDelay, intervalMs, TimeUnit.MILLISECONDS);
        ConsoleUi.info("SQL metrics recorder started (every " + (intervalMs / 1000) + "s).");
    }

    /**
     * Stops sampling immediately.
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
     * Aligns the first sample with the configured interval boundary.
     */
    private long computeInitialDelay() {
        long now = System.currentTimeMillis();
        long next = ((now / intervalMs) + 1) * intervalMs;
        return Math.max(0, next - now);
    }

    /**
     * Records a single online player count sample.
     */
    private void recordOnce() {
        try {
            int totalPlayers = computeTotalPlayers();
            Timestamp recordedAt = Timestamp.from(Instant.now().truncatedTo(ChronoUnit.MINUTES));
            try (Connection connection = openConnection()) {
                ensureTable(connection);
                insertSample(connection, recordedAt, totalPlayers);
            }
        } catch (Exception e) {
            ConsoleUi.error("SQL metrics write failed: " + e.getMessage());
        }
    }

    /**
     * Sums player counts across all known servers.
     */
    private int computeTotalPlayers() throws Exception {
        List<ServerStatus> statuses = statusService.listStatuses();
        int total = 0;
        for (ServerStatus status : statuses) {
            total += Math.max(0, status.getCurrentPlayers());
        }
        return total;
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
     * Creates the metrics table if it does not exist.
     */
    private void ensureTable(Connection connection) throws SQLException {
        if (initialized) {
            return;
        }
        String table = resolveTableName();
        String createSql = "CREATE TABLE IF NOT EXISTS " + table + " ("
            + "id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,"
            + "recorded_at DATETIME NOT NULL,"
            + "online_players INT UNSIGNED NOT NULL DEFAULT 0,"
            + "PRIMARY KEY (id),"
            + "INDEX idx_recorded_at (recorded_at)"
            + ")";
        try (Statement statement = connection.createStatement()) {
            statement.execute(createSql);
        }
        initialized = true;
    }

    /**
     * Inserts a single metrics sample row.
     */
    private void insertSample(Connection connection, Timestamp recordedAt, int onlinePlayers) throws SQLException {
        String table = resolveTableName();
        String insertSql = "INSERT INTO " + table + " (recorded_at, online_players) VALUES (?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(insertSql)) {
            statement.setTimestamp(1, recordedAt);
            statement.setInt(2, onlinePlayers);
            statement.executeUpdate();
        }
    }

    /**
     * Resolves a safe table name from SQL config.
     */
    private String resolveTableName() {
        String table = sqlConfig.getTable();
        if (table == null || table.trim().isEmpty()) {
            return "online_player_minutes";
        }
        String sanitized = table.trim();
        if (!sanitized.matches("[A-Za-z0-9_]+")) {
            ConsoleUi.warn("Invalid SQL table name; using online_player_minutes.");
            return "online_player_minutes";
        }
        return sanitized;
    }
}
