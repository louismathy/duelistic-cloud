package com.duelistic.commands;

import com.duelistic.system.ServerLauncher;
import com.duelistic.system.ServerShutdown;
import com.duelistic.ui.ConsoleUi;

import java.io.IOException;

public class StopServerCommand implements Command{
    private final ServerShutdown shutdown;

    public StopServerCommand(ServerShutdown shutdown) {
        this.shutdown = shutdown;
    }

    @Override
    public void execute(String[] args) {
        if (args.length != 1) {
            printUsage();
            return;
        }
        shutdown.stop(args[0])
                .thenAccept(stopped -> {
                    if (stopped) {
                        ConsoleUi.info("Stopped " + args[0]);
                    } else {
                        ConsoleUi.error("Failed to stop " + args[0]);
                    }
                })
                .exceptionally(ex -> {
                    ConsoleUi.error("Failed to stop " + args[0] + ": " + ex.getMessage());
                    return null;
                });

    }

    @Override
    public String getName() {
        return "stopserver";
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
        return "stopserver <server>";
    }
}
