package com.duelistic.commands;

import java.io.IOException;

import com.duelistic.system.ServerLauncher;
import com.duelistic.ui.ConsoleUi;

public class StartCommand implements Command {
    private final ServerLauncher launcher;

    public StartCommand(ServerLauncher launcher) {
        this.launcher = launcher;
    }

    @Override
    public String getName() {
        return "start";
    }

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
}
