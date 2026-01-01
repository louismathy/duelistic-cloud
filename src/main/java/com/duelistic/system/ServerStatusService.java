package com.duelistic.system;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Properties;

/**
 * Builds real-time server status snapshots from disk and player counts.
 */
public class ServerStatusService {
    private static final int CONNECT_TIMEOUT_MS = 200;
    private static final long COUNTS_TTL_MS = 30_000;
    private final CloudDirectories directories;
    private final ServerPlayerRegistry playerRegistry;

    /**
     * Creates a status service tied to cloud directories and player counts.
     */
    public ServerStatusService(CloudDirectories directories, ServerPlayerRegistry playerRegistry) {
        this.directories = directories;
        this.playerRegistry = playerRegistry;
    }

    /**
     * Lists status snapshots for all known tmp servers.
     */
    public List<ServerStatus> listStatuses() throws IOException {
        // Merge servers discovered on disk with those that only sent counts via plugin.
        List<String> servers = directories.listTmpServers();
        Set<String> allServers = new HashSet<>(servers);
        allServers.addAll(playerRegistry.getServerNames());
        List<String> combined = new ArrayList<>(allServers);
        Collections.sort(combined);
        List<ServerStatus> statuses = new ArrayList<>();
        for (String server : combined) {
            statuses.add(buildStatus(server));
        }
        return statuses;
    }

    /**
     * Builds a status snapshot for a single server.
     */
    private ServerStatus buildStatus(String serverName) {
        // Build a status snapshot from disk config and live player counts.
        TemplateConfig config = loadTemplateConfig(serverName);
        String templateName = readTemplateName(serverName, config);
        int port = readServerPort(serverName);
        ServerPlayerRegistry.PlayerCounts counts = playerRegistry.getCounts(serverName);
        Instant lastUpdatedAt = playerRegistry.getLastUpdatedAt(serverName);
        String displayName = playerRegistry.getDisplayName(serverName);
        boolean countsFresh = counts != null && lastUpdatedAt != null
            && Duration.between(lastUpdatedAt, Instant.now()).toMillis() <= COUNTS_TTL_MS;
        boolean online = port > 0 && isPortOpen("127.0.0.1", port);
        if (countsFresh) {
            // Plugin updates are considered authoritative for online state.
            online = true;
        }
        int currentPlayers = countsFresh ? counts.getCurrentPlayers() : 0;
        int maxPlayers = countsFresh ? counts.getMaxPlayers() : readMaxPlayers(config);
        java.time.Instant startedAt = playerRegistry.getStartedAt(serverName);
        String name = (displayName == null || displayName.trim().isEmpty()) ? serverName : displayName.trim();
        return new ServerStatus(name, templateName, port, online, currentPlayers, maxPlayers, startedAt);
    }

    /**
     * Loads the template config associated with a tmp server.
     */
    private TemplateConfig loadTemplateConfig(String serverName) {
        Path configFile = directories.getTmpServerDir(serverName).resolve("template.yml");
        try {
            return TemplateConfig.loadFrom(configFile);
        } catch (IOException e) {
            // Missing or unreadable template config is tolerated.
            return null;
        }
    }

    /**
     * Resolves a template name from config or the server name prefix.
     */
    private String readTemplateName(String serverName, TemplateConfig config) {
        if (config != null && config.getTemplateName() != null) {
            return config.getTemplateName();
        }
        // Fallback to prefix before the last dash.
        int dash = serverName.lastIndexOf('-');
        if (dash > 0) {
            return serverName.substring(0, dash);
        }
        return "unknown";
    }

    /**
     * Reads max players from the template config, if available.
     */
    private int readMaxPlayers(TemplateConfig config) {
        if (config == null) {
            return 0;
        }
        return Math.max(0, config.getMaxPlayers());
    }

    /**
     * Reads the server port from server.properties.
     */
    private int readServerPort(String serverName) {
        // Port is read from the server.properties of the running server directory.
        Path propertiesFile = directories.getTmpServerDir(serverName).resolve("server.properties");
        if (!Files.exists(propertiesFile)) {
            return -1;
        }
        Properties props = new Properties();
        try (InputStream input = Files.newInputStream(propertiesFile)) {
            props.load(input);
        } catch (IOException e) {
            return -1;
        }
        String value = props.getProperty("server-port");
        if (value == null) {
            return -1;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Performs a quick TCP connect to determine if a port is open.
     */
    private boolean isPortOpen(String host, int port) {
        // Quick TCP check to determine if the server is listening.
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MS);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
