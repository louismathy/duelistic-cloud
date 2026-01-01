package com.duelistic.system;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.duelistic.ui.ConsoleUi;

/**
 * Periodically checks servers for crashes and scales templates as needed.
 */
public class ServerAutoRenewService {
    private final CloudDirectories directories;
    private final ServerStatusService statusService;
    private final ServerLauncher launcher;
    private final ServerProcessManager processManager;
    private final ServerPlayerRegistry playerRegistry;
    private final Map<String, Boolean> lastOnline = new HashMap<>();
    private final ScheduledExecutorService executor;
    private final long intervalMs;

    /**
     * Creates a new auto-renew service with the provided dependencies.
     */
    public ServerAutoRenewService(CloudDirectories directories,
                                  ServerStatusService statusService,
                                  ServerLauncher launcher,
                                  ServerProcessManager processManager,
                                  ServerPlayerRegistry playerRegistry,
                                  long intervalMs) {
        this.directories = directories;
        this.statusService = statusService;
        this.launcher = launcher;
        this.processManager = processManager;
        this.playerRegistry = playerRegistry;
        this.intervalMs = intervalMs;
        this.executor = Executors.newSingleThreadScheduledExecutor();
    }

    /**
     * Starts the periodic health and scaling checks.
     */
    public void start() {
        // Periodically check status to restart crashed servers and scale up.
        executor.scheduleAtFixedRate(this::checkServers, intervalMs, intervalMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Stops the scheduled check task immediately.
     */
    public void stop() {
        executor.shutdownNow();
    }

    /**
     * Runs a single cycle of crash detection and scaling.
     */
    private void checkServers() {
        try {
            List<ServerStatus> statuses = statusService.listStatuses();
            Set<String> seen = new HashSet<>();
            for (ServerStatus status : statuses) {
                String name = status.getName();
                seen.add(name);
                boolean wasOnline = lastOnline.getOrDefault(name, false);
                boolean isOnline = status.isOnline();
                lastOnline.put(name, isOnline);
                if (wasOnline && !isOnline) {
                    // Server went offline since last check, treat as crash.
                    handleCrash(status);
                }
            }
            lastOnline.keySet().retainAll(seen);
            playerRegistry.prune(seen);
            // Ensure template min/max requirements are respected.
            checkScaleUp(statuses);
        } catch (IOException e) {
            ConsoleUi.error("Auto-renew check failed: " + e.getMessage());
        }
    }

    /**
     * Handles a server that was online and now appears offline.
     */
    private void handleCrash(ServerStatus status) {
        String serverName = status.getName();
        String template = status.getTemplate();
        ConsoleUi.warn("Detected shutdown / crash for " + serverName + " (template " + template + ").");
        try {
            processManager.stopServer(serverName);
            directories.deleteTmpServer(serverName);
            playerRegistry.removeServer(serverName);
            int remaining = countServersForTemplate(template);
            TemplateConfig config = TemplateConfig.loadFrom(directories.getTemplateConfigFile(template));
            if (remaining < config.getServerMin()) {
                ConsoleUi.info("Restarting a " + template + " server to maintain minimum.");
                launcher.startTemplateServer(template);
            }
        } catch (IOException e) {
            ConsoleUi.error("Auto-renew failed for " + serverName + ": " + e.getMessage());
        }
    }

    /**
     * Counts tmp servers that belong to a template.
     */
    private int countServersForTemplate(String template) throws IOException {
        // Count tmp servers that belong to a template.
        int count = 0;
        for (String serverName : directories.listTmpServers()) {
            if (template.equals(readTemplateName(serverName))) {
                count++;
            }
        }
        return count;
    }

    /**
     * Attempts to resolve a template name for a server directory.
     */
    private String readTemplateName(String serverName) {
        try {
            TemplateConfig config = TemplateConfig.loadFrom(directories.getTmpServerDir(serverName).resolve("template.yml"));
            if (config.getTemplateName() != null) {
                return config.getTemplateName();
            }
        } catch (IOException e) {
            // ignore and fall back
        }
        // Fallback to prefix before last dash.
        int dash = serverName.lastIndexOf('-');
        if (dash > 0) {
            return serverName.substring(0, dash);
        }
        return "unknown";
    }

    /**
     * Starts new servers if all instances of a template are full.
     */
    private void checkScaleUp(List<ServerStatus> statuses) throws IOException {
        // If all servers of a template are full, start another instance (if allowed).
        Map<String, List<ServerStatus>> byTemplate = new HashMap<>();
        for (ServerStatus status : statuses) {
            String template = status.getTemplate();
            byTemplate.computeIfAbsent(template, key -> new java.util.ArrayList<>()).add(status);
        }
        for (Map.Entry<String, List<ServerStatus>> entry : byTemplate.entrySet()) {
            String template = entry.getKey();
            List<ServerStatus> templateServers = entry.getValue();
            if (templateServers.isEmpty()) {
                continue;
            }
            TemplateConfig config;
            try {
                config = TemplateConfig.loadFrom(directories.getTemplateConfigFile(template));
            } catch (IOException e) {
                continue;
            }
            int serverMax = config.getServerMax();
            if (serverMax <= 0 || templateServers.size() >= serverMax) {
                continue;
            }
            boolean allFull = true;
            for (ServerStatus status : templateServers) {
                int maxPlayers = status.getMaxPlayers();
                if (maxPlayers <= 0) {
                    maxPlayers = config.getMaxPlayers();
                }
                if (maxPlayers <= 0 || status.getCurrentPlayers() < maxPlayers) {
                    allFull = false;
                    break;
                }
            }
            if (allFull) {
                ConsoleUi.info("All " + template + " servers are full. Scaling up...");
                launcher.startTemplateServer(template);
            }
        }
    }
}
