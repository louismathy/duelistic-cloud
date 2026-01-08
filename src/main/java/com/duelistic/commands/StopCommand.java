package com.duelistic.commands;

import java.io.IOException;

import com.duelistic.system.ServerAutoRenewService;
import com.duelistic.system.ServerShutdown;
import com.duelistic.system.OnlinePlayerMetricsRecorder;
import com.duelistic.system.DashboardMetricsRecorder;
import com.duelistic.ui.ConsoleUi;
import com.duelistic.system.TemplateSqlSyncService;
import com.duelistic.system.BanCleanupService;
import com.duelistic.system.ServerSqlSyncService;
import com.duelistic.http.CloudHttpServer;

/**
 * Stops all running servers and background services.
 */
public class StopCommand implements Command {
    private final CommandSystem commandSystem;
    private final ServerShutdown shutdown;
    private final ServerAutoRenewService autoRenewService;
    private final OnlinePlayerMetricsRecorder metricsRecorder;
    private final TemplateSqlSyncService templateSqlSyncService;
    private final DashboardMetricsRecorder dashboardMetricsRecorder;
    private final BanCleanupService banCleanupService;
    private final ServerSqlSyncService serverSqlSyncService;
    private final CloudHttpServer httpServer;

    /**
     * Creates the stop command with all shutdown dependencies.
     */
    public StopCommand(CommandSystem commandSystem,
                       ServerShutdown shutdown,
                       ServerAutoRenewService autoRenewService,
                       OnlinePlayerMetricsRecorder metricsRecorder,
                       TemplateSqlSyncService templateSqlSyncService,
                       DashboardMetricsRecorder dashboardMetricsRecorder,
                       BanCleanupService banCleanupService,
                       ServerSqlSyncService serverSqlSyncService,
                       CloudHttpServer httpServer) {
        this.commandSystem = commandSystem;
        this.shutdown = shutdown;
        this.autoRenewService = autoRenewService;
        this.metricsRecorder = metricsRecorder;
        this.templateSqlSyncService = templateSqlSyncService;
        this.dashboardMetricsRecorder = dashboardMetricsRecorder;
        this.banCleanupService = banCleanupService;
        this.serverSqlSyncService = serverSqlSyncService;
        this.httpServer = httpServer;
    }

    /**
     * Returns the CLI command name.
     */
    @Override
    public String getName() {
        return "stop";
    }

    /**
     * Stops servers, background services, and the CLI loop.
     */
    @Override
    public void execute(String[] args) {
        ConsoleUi.info("Stopping Duelistic Cloud...");
        try {
            int stopped = shutdown.stopAll();
            ConsoleUi.success("Stopped " + stopped + " servers.");
        } catch (IOException e) {
            ConsoleUi.error("Failed to delete tmp servers: " + e.getMessage());
        }
        autoRenewService.stop();
        metricsRecorder.stop();
        templateSqlSyncService.stop();
        dashboardMetricsRecorder.stop();
        banCleanupService.stop();
        serverSqlSyncService.stop();

        if (httpServer != null) {
            httpServer.stop();
        }
        commandSystem.stop();
    }
}
