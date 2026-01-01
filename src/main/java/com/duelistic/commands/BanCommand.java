package com.duelistic.commands;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;

import com.duelistic.system.SqlConfig;
import com.duelistic.ui.ConsoleUi;

/**
 * Adds a ban entry to the SQL store.
 */
public class BanCommand implements Command {
    private final SqlConfig sqlConfig;

    /**
     * Creates the ban command with SQL configuration.
     */
    public BanCommand(SqlConfig sqlConfig) {
        this.sqlConfig = sqlConfig;
    }

    /**
     * Returns the CLI command name.
     */
    @Override
    public String getName() {
        return "ban";
    }

    /**
     * Validates input and writes a new ban record.
     */
    @Override
    public void execute(String[] args) {
        if (args.length < 2) {
            printUsage();
            return;
        }
        if (sqlConfig == null || !sqlConfig.isEnabled()) {
            ConsoleUi.warn("SQL is disabled (system/sql.yml).");
            return;
        }
        if (!isConfigValid()) {
            ConsoleUi.warn("SQL is enabled but config is incomplete.");
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

        String username = args[0].trim();
        if (username.isEmpty()) {
            ConsoleUi.warn("Username cannot be empty.");
            return;
        }

        int lengthMinutes = parseLengthMinutes(args[1]);
        if (lengthMinutes < 0) {
            ConsoleUi.warn("Length must be a non-negative number of minutes.");
            return;
        }

        String reason = buildReason(args);
        try (Connection connection = openConnection()) {
            ensureTable(connection);
            insertBan(connection, username, reason, lengthMinutes);
            ConsoleUi.success("Ban added for " + username + ".");
        } catch (Exception e) {
            ConsoleUi.error("Failed to add ban: " + e.getMessage());
        }
    }

    /**
     * Ensures required SQL config values are present.
     */
    private boolean isConfigValid() {
        return sqlConfig.getUrl() != null && !sqlConfig.getUrl().trim().isEmpty();
    }

    /**
     * Parses the ban length in minutes.
     */
    private int parseLengthMinutes(String value) {
        try {
            int parsed = Integer.parseInt(value.trim());
            return Math.max(parsed, 0);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Builds the ban reason from remaining args.
     */
    private String buildReason(String[] args) {
        if (args.length <= 2) {
            return "Unspecified";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 2; i < args.length; i++) {
            if (i > 2) {
                builder.append(' ');
            }
            builder.append(args[i]);
        }
        String reason = builder.toString().trim();
        return reason.isEmpty() ? "Unspecified" : reason;
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
     * Creates the bans table if it does not exist.
     */
    private void ensureTable(Connection connection) throws SQLException {
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
    }

    /**
     * Inserts a ban row into the SQL table.
     */
    private void insertBan(Connection connection, String username, String reason, int lengthMinutes) throws SQLException {
        String insertSql = "INSERT INTO active_bans (username, reason, started_at, length_minutes) VALUES (?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(insertSql)) {
            statement.setString(1, username);
            statement.setString(2, reason);
            statement.setTimestamp(3, Timestamp.from(Instant.now()));
            statement.setInt(4, lengthMinutes);
            statement.executeUpdate();
        }
    }

    /**
     * Prints CLI usage for the command.
     */
    private void printUsage() {
        ConsoleUi.section("Usage");
        ConsoleUi.item("ban <username> <length_minutes> <reason>");
    }
}
