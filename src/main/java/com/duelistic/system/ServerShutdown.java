package com.duelistic.system;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
    public int stopAll() throws IOException {
        List<String> servers = directories.listTmpServers();
        if (servers.isEmpty()) {
            ConsoleUi.info("No servers to stop.");
            return 0;
        }

        for (String server : servers) {
            processManager.stopServer(server);
        }

        ScheduledExecutorService scheduler =
                Executors.newSingleThreadScheduledExecutor();

        scheduler.schedule(() -> {
            try {
                directories.deleteTmp();
            } catch (IOException e) {
                ConsoleUi.error("Failed to delete tmp directory: " + e.getMessage());
            }
        }, 5, TimeUnit.SECONDS);

        scheduler.shutdown();
        return servers.size();
    }

    /**
     * Stops single tmp server
     * @param serverName
     * @return if stop was successful
     */
    public boolean stop(String serverName) throws IOException {
        List<String> servers = directories.listTmpServers();
        if (!servers.contains(serverName))
            return false;
        processManager.stopServer(serverName);
        ScheduledExecutorService scheduler =
                Executors.newSingleThreadScheduledExecutor();


        scheduler.schedule(() -> {
            try {
                directories.deleteTmpServer(serverName);
            } catch (IOException e) {
                ConsoleUi.error("Failed to delete tmp directory: " + e.getMessage());
            }
        }, 5, TimeUnit.SECONDS);
        return true;
    }

}
