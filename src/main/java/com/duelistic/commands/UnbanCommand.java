package com.duelistic.commands;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import com.duelistic.system.SqlConfig;
import com.duelistic.ui.ConsoleUi;

/**
 * Removes an active ban entry from SQL.
 */
public class UnbanCommand implements Command {
    private final SqlConfig sqlConfig;

    /**
     * Creates the unban command with SQL configuration.
     */
    public UnbanCommand(SqlConfig sqlConfig) {
        this.sqlConfig = sqlConfig;
    }

    /**
     * Returns the CLI command name.
     */
    @Override
    public String getName() {
        return "unban";
    }

    @Override
    public String getUsage() {
        return "unban <username>";
    }

    /**
     * Validates input and removes an existing ban.
     */
    @Override
    public void execute(String[] args) {
        if (args.length < 1) {
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

        try (Connection connection = openConnection()) {
            ensureTable(connection);
            int deleted = removeBan(connection, username);
            if (deleted > 0) {
                ConsoleUi.success("Ban removed for " + username + ".");
            } else {
                ConsoleUi.warn("No active ban found for " + username + ".");
            }
        } catch (Exception e) {
            ConsoleUi.error("Failed to remove ban: " + e.getMessage());
        }
    }

    /**
     * Ensures required SQL config values are present.
     */
    private boolean isConfigValid() {
        return sqlConfig.getUrl() != null && !sqlConfig.getUrl().trim().isEmpty();
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
     * Deletes a ban row for the given username.
     */
    private int removeBan(Connection connection, String username) throws SQLException {
        String deleteSql = "DELETE FROM active_bans WHERE username = ?";
        try (PreparedStatement statement = connection.prepareStatement(deleteSql)) {
            statement.setString(1, username);
            return statement.executeUpdate();
        }
    }

    /**
     * Prints CLI usage for the command.
     */
    private void printUsage() {
        ConsoleUi.section("Usage");
        ConsoleUi.item(getUsage());
    }
}
