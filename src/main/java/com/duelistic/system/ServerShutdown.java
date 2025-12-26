package com.duelistic.system;

import java.io.IOException;
import java.util.List;

import com.duelistic.ui.ConsoleUi;

public class ServerShutdown {
    private final CloudDirectories directories;
    private final ServerProcessManager processManager;

    public ServerShutdown(CloudDirectories directories, ServerProcessManager processManager) {
        this.directories = directories;
        this.processManager = processManager;
    }

    public int stopAll() throws IOException {
        // Stop all tmp servers and clean the tmp directory.
        List<String> servers = directories.listTmpServers();
        if (servers.isEmpty()) {
            ConsoleUi.info("No servers to stop.");
        }
        for (String server : servers) {
            processManager.stopServer(server);
        }
        directories.deleteTmp();
        return servers.size();
    }
}
