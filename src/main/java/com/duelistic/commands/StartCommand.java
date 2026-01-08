package com.duelistic.commands;

import java.io.IOException;

import com.duelistic.system.ServerLauncher;
import com.duelistic.ui.ConsoleUi;

/**
 * Starts all template servers from the CLI.
 */
public class StartCommand implements Command {
    private final ServerLauncher launcher;

    /**
     * Creates the start command with a server launcher.
     */
    public StartCommand(ServerLauncher launcher) {
        this.launcher = launcher;
    }

    /**
     * Returns the CLI command name.
     */
    @Override
    public String getName() {
        return "start";
    }



    /**
     * Starts all configured template servers.
     */
    @Override
    public void execute(String[] args) {
        try {
            ConsoleUi.info("Starting template servers...");
            int created = launcher.startAll();
            ConsoleUi.success("Created " + created + " temporary servers.");
        } catch (IOException e) {
            ConsoleUi.error("Start failed: " + e.getMessage());
        }
    }

    @Override
    public String getUsage() {
        return "start";
    }
}
