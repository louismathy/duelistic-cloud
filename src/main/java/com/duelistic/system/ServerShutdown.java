package com.duelistic.system;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.*;

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
    public CompletableFuture<Integer> stopAll() {
        return CompletableFuture.supplyAsync(() -> {
            try {
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

                CompletableFuture<Integer> result = new CompletableFuture<>();

                scheduler.schedule(() -> {
                    try {
                        directories.deleteTmp();
                        result.complete(servers.size());
                    } catch (IOException e) {
                        ConsoleUi.error("Failed to delete tmp directory: " + e.getMessage());
                        result.completeExceptionally(e);
                    } finally {
                        scheduler.shutdown();
                    }
                }, 5, TimeUnit.SECONDS);

                return result.join();
            } catch (IOException e) {
                throw new CompletionException(e);
            }
        });
    }


    /**
     * Stops single tmp server
     * @param serverName
     * @return if stop was successful
     */
    public CompletableFuture<Boolean> stop(String serverName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<String> servers = directories.listTmpServers();
                if (!servers.contains(serverName)) {
                    return false;
                }

                processManager.stopServer(serverName);

                ScheduledExecutorService scheduler =
                        Executors.newSingleThreadScheduledExecutor();

                CompletableFuture<Boolean> result = new CompletableFuture<>();

                scheduler.schedule(() -> {
                    try {
                        directories.deleteTmpServer(serverName);
                        result.complete(true);
                    } catch (IOException e) {
                        ConsoleUi.error("Failed to delete tmp directory: " + e.getMessage());
                        result.completeExceptionally(e);
                    } finally {
                        scheduler.shutdown();
                    }
                }, 5, TimeUnit.SECONDS);

                return result.join();
            } catch (IOException e) {
                throw new CompletionException(e);
            }
        });
    }

}
