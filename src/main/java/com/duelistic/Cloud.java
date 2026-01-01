package com.duelistic;


import java.util.Scanner;

import com.duelistic.commands.*;
import com.duelistic.http.CloudHttpServer;
import com.duelistic.system.CloudConfig;
import com.duelistic.system.CloudDirectories;
import com.duelistic.system.BanCleanupService;
import com.duelistic.system.DashboardMetricsRecorder;
import com.duelistic.system.OnlinePlayerMetricsRecorder;
import com.duelistic.system.ScreenServerProcessManager;
import com.duelistic.system.ServerAutoRenewService;
import com.duelistic.system.ServerLauncher;
import com.duelistic.system.ServerPlayerRegistry;
import com.duelistic.system.ServerProcessManager;
import com.duelistic.system.ServerShutdown;
import com.duelistic.system.ServerStatusService;
import com.duelistic.system.ServerSqlSyncService;
import com.duelistic.system.SqlConfig;
import com.duelistic.system.TemplateSqlSyncService;
import com.duelistic.ui.ConsoleUi;
import com.duelistic.util.VirtualResourceUtil;

/**
 * Application entry point that wires core services, starts background jobs,
 * and exposes the CLI for managing cloud servers.
 */
public class Cloud
{
    private static Cloud instance;
    private Scanner keyScanner;
    private CommandRegistry commandRegistry;
    private CommandSystem commandSystem;
    private CloudDirectories cloudDirectories;
    private CloudConfig cloudConfig;
    private ServerProcessManager processManager;
    private ServerLauncher serverLauncher;
    private ServerShutdown serverShutdown;
    private ServerStatusService statusService;
    private ServerAutoRenewService autoRenewService;
    private ServerPlayerRegistry playerRegistry;
    private OnlinePlayerMetricsRecorder metricsRecorder;
    private TemplateSqlSyncService templateSqlSyncService;
    private DashboardMetricsRecorder dashboardMetricsRecorder;
    private BanCleanupService banCleanupService;
    private ServerSqlSyncService serverSqlSyncService;
    private VirtualResourceUtil virtualResourceUtil;
    private CloudHttpServer httpServer;
    /**
     * Bootstraps the cloud runtime and starts all recurring services.
     *
     * @param args CLI args (unused).
     */
    public static void main(String[] args)
    {
        ConsoleUi.banner("Duelistic Cloud");
        ConsoleUi.info("Starting up...");

        instance = new Cloud();
        // Core service wiring.
        instance.keyScanner = new Scanner(System.in);
        instance.cloudDirectories = new CloudDirectories();
        instance.processManager = new ScreenServerProcessManager();
        instance.playerRegistry = new ServerPlayerRegistry();
        instance.serverLauncher = new ServerLauncher(instance.cloudDirectories, instance.processManager, instance.playerRegistry);
        instance.serverShutdown = new ServerShutdown(instance.cloudDirectories, instance.processManager);
        instance.statusService = new ServerStatusService(instance.cloudDirectories, instance.playerRegistry);
        try {
            instance.cloudDirectories.ensureExists();
            SqlConfig.writeDefaultIfMissing(instance.cloudDirectories.getSqlConfigFile());
            CloudConfig.writeDefaultIfMissing(instance.cloudDirectories.getConfigFile());
        } catch (Exception e) {
            ConsoleUi.error("Failed to create system directory: " + e.getMessage());
            return;
        }
        instance.cloudConfig = CloudConfig.loadFrom(instance.cloudDirectories.getConfigFile());
        SqlConfig sqlConfig = SqlConfig.loadFrom(instance.cloudDirectories.getSqlConfigFile());

        instance.virtualResourceUtil = new VirtualResourceUtil(instance.cloudConfig.getVirtualRamLimitMb(), instance.statusService, instance.cloudDirectories);
        instance.autoRenewService = new ServerAutoRenewService(instance.cloudDirectories,
            instance.statusService,
            instance.serverLauncher,
            instance.processManager,
            instance.playerRegistry,
            instance.cloudConfig.getAutoRenewIntervalMs());
        instance.templateSqlSyncService = new TemplateSqlSyncService(instance.cloudDirectories,
            sqlConfig,
            instance.cloudConfig.getTemplateSyncIntervalMs());
        instance.dashboardMetricsRecorder = new DashboardMetricsRecorder(instance.statusService,
            sqlConfig,
            instance.cloudConfig.getDashboardMetricsIntervalMs());
        instance.banCleanupService = new BanCleanupService(sqlConfig, instance.cloudConfig.getBanCleanupIntervalMs());
        instance.serverSqlSyncService = new ServerSqlSyncService(instance.statusService,
            sqlConfig,
            instance.cloudConfig.getServerSyncIntervalMs());
        instance.metricsRecorder = new OnlinePlayerMetricsRecorder(instance.statusService,
            sqlConfig,
            instance.cloudConfig.getOnlineMetricsIntervalMs());
        if (instance.cloudConfig.isHttpApiEnabled()) {
            instance.httpServer = new CloudHttpServer(instance.statusService,
                instance.serverShutdown,
                instance.playerRegistry,
                instance.cloudConfig.getHttpApiPort());
            instance.httpServer.start();
        }
        // Register CLI commands.
        instance.commandRegistry = new CommandRegistry();
        instance.commandSystem = new CommandSystem(instance.keyScanner, instance.commandRegistry);
        instance.commandRegistry.register(new HelpCommand(instance.commandRegistry));
        instance.commandRegistry.register(new SetupCommand(instance.cloudDirectories, instance.keyScanner));
        instance.commandRegistry.register(new StartCommand(instance.serverLauncher));
        instance.commandRegistry.register(new TemplateCommand(instance.cloudDirectories, instance.keyScanner));
        instance.commandRegistry.register(new ServerListCommand(instance.statusService));
        instance.commandRegistry.register(new BanCommand(sqlConfig));
        instance.commandRegistry.register(new UnbanCommand(sqlConfig));
        instance.commandRegistry.register(new ReportCommand(sqlConfig));
        instance.commandRegistry.register(new StopCommand(instance.commandSystem, instance.serverShutdown, instance.autoRenewService, instance.metricsRecorder, instance.templateSqlSyncService, instance.dashboardMetricsRecorder, instance.banCleanupService, instance.serverSqlSyncService, instance.httpServer));
        instance.commandRegistry.register(new ResourcesCommand());
        instance.commandRegistry.register(new StartServerCommand(instance.serverLauncher));
        instance.commandRegistry.register(new StopServerCommand(instance.serverShutdown));
        ConsoleUi.logo();
        ConsoleUi.success("Cloud core initialized.");
        // Periodic health and scaling checks.
        instance.autoRenewService.start();
        instance.metricsRecorder.start();
        instance.templateSqlSyncService.start();
        instance.dashboardMetricsRecorder.start();
        instance.banCleanupService.start();
        instance.serverSqlSyncService.start();
        ConsoleUi.info("Type 'help' for commands.");
        instance.commandSystem.start();
    }

    /**
     * Returns the singleton instance created at startup.
     *
     * @return the running Cloud instance, or null if not initialized.
     */
    public static Cloud getInstance() {
        return instance;
    }

    /**
     * Returns the shared scanner used for interactive CLI input.
     *
     * @return scanner bound to stdin.
     */
    public Scanner getKeyScanner() {
        return keyScanner;
    }

    /**
     * Returns the registry of available CLI commands.
     *
     * @return command registry.
     */
    public CommandRegistry getCommandRegistry() {
        return commandRegistry;
    }

    /**
     * Returns the command system driving the CLI loop.
     *
     * @return command system instance.
     */
    public CommandSystem getCommandSystem() {
        return commandSystem;
    }

    /**
     * Returns the directory helper for templates and tmp servers.
     *
     * @return cloud directory helper.
     */
    public CloudDirectories getCloudDirectories() {
        return cloudDirectories;
    }

    /**
     * Returns the loaded cloud configuration.
     */
    public CloudConfig getCloudConfig() {
        return cloudConfig;
    }

    /**
     * Returns the virtual resource helper utility
     * @return virtual resource util
     */
    public VirtualResourceUtil getVirtualResourceUtil() {
        return virtualResourceUtil;
    }
}
