package com.duelistic.commands;

import com.duelistic.Cloud;
import com.duelistic.system.ServerLauncher;
import com.duelistic.ui.ConsoleUi;

import java.io.IOException;

public class StartServerCommand implements Command{
    private final ServerLauncher launcher;

    public StartServerCommand(ServerLauncher launcher) {
        this.launcher = launcher;
    }

    @Override
    public void execute(String[] args) {
        if (args.length != 1) {
            printUsage();
            return;
        }
        try {
            launcher.startTemplateServer(args[0]);
        } catch (IOException e) {
            ConsoleUi.error("Failed to start server: " + e.getMessage());
        }
    }

    @Override
    public String getName() {
        return "startserver";
    }

    /**
     * Prints CLI usage for the command.
     */
    private void printUsage() {
        ConsoleUi.section("Usage");
        ConsoleUi.item("startserver <template>");
    }
}
