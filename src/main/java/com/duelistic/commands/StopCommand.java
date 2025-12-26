package com.duelistic.commands;

import java.io.IOException;

import com.duelistic.system.ServerAutoRenewService;
import com.duelistic.system.ServerShutdown;
import com.duelistic.http.CloudHttpServer;
import com.duelistic.ui.ConsoleUi;

public class StopCommand implements Command {
    private final CommandSystem commandSystem;
    private final ServerShutdown shutdown;
    private final CloudHttpServer httpServer;
    private final ServerAutoRenewService autoRenewService;

    public StopCommand(CommandSystem commandSystem, ServerShutdown shutdown, CloudHttpServer httpServer, ServerAutoRenewService autoRenewService) {
        this.commandSystem = commandSystem;
        this.shutdown = shutdown;
        this.httpServer = httpServer;
        this.autoRenewService = autoRenewService;
    }

    @Override
    public String getName() {
        return "stop";
    }

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
        httpServer.stop();
        commandSystem.stop();
    }
}
