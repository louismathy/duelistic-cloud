package com.duelistic;


import java.util.Scanner;

import com.duelistic.commands.CommandRegistry;
import com.duelistic.commands.CommandSystem;
import com.duelistic.commands.HelpCommand;
import com.duelistic.commands.ServerListCommand;
import com.duelistic.commands.SetupCommand;
import com.duelistic.commands.StartCommand;
import com.duelistic.commands.StopCommand;
import com.duelistic.commands.TemplateCommand;
import com.duelistic.http.CloudHttpServer;
import com.duelistic.system.CloudDirectories;
import com.duelistic.system.ScreenServerProcessManager;
import com.duelistic.system.ServerAutoRenewService;
import com.duelistic.system.ServerLauncher;
import com.duelistic.system.ServerPlayerRegistry;
import com.duelistic.system.ServerProcessManager;
import com.duelistic.system.ServerShutdown;
import com.duelistic.system.ServerStatusService;
import com.duelistic.ui.ConsoleUi;

public class Cloud
{
    private static final String HTTP_HOST = "127.0.0.1";
    private static final int HTTP_PORT = 8080;
    private static final long AUTO_RENEW_INTERVAL_MS = 5000;
    private static Cloud instance;
    private Scanner keyScanner;
    private CommandRegistry commandRegistry;
    private CommandSystem commandSystem;
    private CloudDirectories cloudDirectories;
    private ServerProcessManager processManager;
    private ServerLauncher serverLauncher;
    private ServerShutdown serverShutdown;
    private CloudHttpServer httpServer;
    private ServerStatusService statusService;
    private ServerAutoRenewService autoRenewService;
    private ServerPlayerRegistry playerRegistry;
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
        instance.httpServer = new CloudHttpServer(instance.cloudDirectories, instance.serverLauncher, instance.serverShutdown, instance.statusService, instance.playerRegistry);
        instance.autoRenewService = new ServerAutoRenewService(instance.cloudDirectories, instance.statusService, instance.serverLauncher, instance.processManager, instance.playerRegistry, AUTO_RENEW_INTERVAL_MS);
        try {
            instance.cloudDirectories.ensureExists();
        } catch (Exception e) {
            ConsoleUi.error("Failed to create system directory: " + e.getMessage());
            return;
        }
        // Register CLI commands.
        instance.commandRegistry = new CommandRegistry();
        instance.commandSystem = new CommandSystem(instance.keyScanner, instance.commandRegistry);
        instance.commandRegistry.register(new HelpCommand(instance.commandRegistry));
        instance.commandRegistry.register(new SetupCommand(instance.cloudDirectories, instance.keyScanner));
        instance.commandRegistry.register(new StartCommand(instance.serverLauncher));
        instance.commandRegistry.register(new TemplateCommand(instance.cloudDirectories, instance.keyScanner));
        instance.commandRegistry.register(new ServerListCommand(instance.statusService));
        instance.commandRegistry.register(new StopCommand(instance.commandSystem, instance.serverShutdown, instance.httpServer, instance.autoRenewService));

        ConsoleUi.logo();
        ConsoleUi.success("Cloud core initialized.");
        try {
            // Local HTTP API for status and plugin updates.
            instance.httpServer.start(HTTP_HOST, HTTP_PORT);
            ConsoleUi.info("HTTP API running on http://" + HTTP_HOST + ":" + HTTP_PORT);
        } catch (Exception e) {
            ConsoleUi.error("Failed to start HTTP API: " + e.getMessage());
        }
        // Periodic health and scaling checks.
        instance.autoRenewService.start();
        ConsoleUi.info("Type 'help' for commands.");
        instance.commandSystem.start();
    }

    public static Cloud getInstance() {
        return instance;
    }

    public Scanner getKeyScanner() {
        return keyScanner;
    }

    public CommandRegistry getCommandRegistry() {
        return commandRegistry;
    }

    public CommandSystem getCommandSystem() {
        return commandSystem;
    }

    public CloudDirectories getCloudDirectories() {
        return cloudDirectories;
    }
}
