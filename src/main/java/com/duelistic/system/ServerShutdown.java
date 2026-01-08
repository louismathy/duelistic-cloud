package com.duelistic.system;

import java.io.IOException;
import java.util.List;

import com.duelistic.ui.ConsoleUi;

/**
 * Stops all running servers and cleans the tmp directory.
 */
public class ServerShutdown {
    private final CloudDirectories directories;
    private final ServerProcessManager processManager;

    /**
     * Creates a shutdown helper for managed servers.
     */
    public ServerShutdown(CloudDirectories directories, ServerProcessManager processManager) {
        this.directories = directories;
        this.processManager = processManager;
    }

    /**
     * Stops all tmp servers and deletes their files.
     *
     * @return number of servers that were present.
     */
    public int stopAll() throws IOException, InterruptedException {
        // Stop all tmp servers and clean the tmp directory.
        List<String> servers = directories.listTmpServers();
        if (servers.isEmpty()) {
            ConsoleUi.info("No servers to stop.");
        }
        for (String server : servers) {
            processManager.stopServer(server);
        }
        Thread.sleep(1000 * 5);
        directories.deleteTmp();
        return servers.size();
    }

    /**
     * Stops single tmp server
     * @param serverName
     * @return if stop was successful
     */
    public boolean stop(String serverName) throws IOException, InterruptedException {
        List<String> servers = directories.listTmpServers();
        if (!servers.contains(serverName))
            return false;
        processManager.stopServer(serverName);
        Thread.sleep(1000 * 5);
        directories.deleteTmpServer(serverName);
        return true;
    }
}
