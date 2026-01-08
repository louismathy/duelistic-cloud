package com.duelistic.commands;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import com.duelistic.system.SqlConfig;
import com.duelistic.ui.ConsoleUi;

/**
 * Files player reports into the SQL database.
 */
public class ReportCommand implements Command {
    private final SqlConfig sqlConfig;

    /**
     * Creates the report command with SQL configuration.
     */
    public ReportCommand(SqlConfig sqlConfig) {
        this.sqlConfig = sqlConfig;
    }

    /**
     * Returns the CLI command name.
     */
    @Override
    public String getName() {
        return "report";
    }


    /**
     * Validates input and inserts a report row.
     */
    @Override
    public void execute(String[] args) {
        if (args.length < 4) {
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

        String reportedPlayer = args[0].trim();
        String reporterPlayer = args[1].trim();
        if (reportedPlayer.isEmpty() || reporterPlayer.isEmpty()) {
            ConsoleUi.warn("Reported and reporter player names cannot be empty.");
            return;
        }

        String reason = args[2].trim();
        String location = buildLocation(args);
        if (reason.isEmpty() || location.isEmpty()) {
            ConsoleUi.warn("Reason and location cannot be empty.");
            return;
        }

        try (Connection connection = openConnection()) {
            ensureTable(connection);
            insertReport(connection, reportedPlayer, reporterPlayer, reason, location);
            ConsoleUi.success("Report filed for " + reportedPlayer + ".");
        } catch (Exception e) {
            ConsoleUi.error("Failed to file report: " + e.getMessage());
        }
    }

    /**
     * Ensures required SQL config values are present.
     */
    private boolean isConfigValid() {
        return sqlConfig.getUrl() != null && !sqlConfig.getUrl().trim().isEmpty();
    }

    /**
     * Builds the location string from remaining args.
     */
    private String buildLocation(String[] args) {
        if (args.length <= 3) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 3; i < args.length; i++) {
            if (i > 3) {
                builder.append(' ');
            }
            builder.append(args[i]);
        }
        return builder.toString().trim();
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
     * Creates the reports table if it does not exist.
     */
    private void ensureTable(Connection connection) throws SQLException {
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
     * Inserts a report row into SQL.
     */
    private void insertReport(Connection connection,
                              String reportedPlayer,
                              String reporterPlayer,
                              String reason,
                              String location) throws SQLException {
        String insertSql = "INSERT INTO player_reports (reported_player, reporter_player, reason, location) "
            + "VALUES (?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(insertSql)) {
            statement.setString(1, reportedPlayer);
            statement.setString(2, reporterPlayer);
            statement.setString(3, reason);
            statement.setString(4, location);
            statement.executeUpdate();
        }
    }

    /**
     * Prints CLI usage for the command.
     */
    private void printUsage() {
        ConsoleUi.section("Usage");
        ConsoleUi.item(getUsage());
    }

    @Override
    public String getUsage() {
        return "report <reported_player> <reporter_player> <reason> <location>";
    }
}
