package com.duelistic.commands;

import java.io.IOException;
import java.util.List;

import com.duelistic.system.ServerStatus;
import com.duelistic.system.ServerStatusService;
import com.duelistic.ui.ConsoleUi;

public class ServerListCommand implements Command {
    private final ServerStatusService statusService;

    public ServerListCommand(ServerStatusService statusService) {
        this.statusService = statusService;
    }

    @Override
    public String getName() {
        return "servers";
    }

    @Override
    public void execute(String[] args) {
        try {
            List<ServerStatus> statuses = statusService.listStatuses();
            if (statuses.isEmpty()) {
                ConsoleUi.warn("No temporary servers found.");
                return;
            }
            ConsoleUi.section("Temporary servers");
            for (ServerStatus status : statuses) {
                String online = status.isOnline() ? "online" : "offline";
                ConsoleUi.item(status.getName()
                    + " | template=" + status.getTemplate()
                    + " | port=" + status.getPort()
                    + " | players=" + status.getCurrentPlayers() + "/" + status.getMaxPlayers()
                    + " | " + online);
            }
        } catch (IOException e) {
            ConsoleUi.error("Failed to list servers: " + e.getMessage());
        }
    }
}
