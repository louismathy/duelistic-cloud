package com.duelistic.system;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Properties;

public class ServerStatusService {
    private static final int CONNECT_TIMEOUT_MS = 200;
    private final CloudDirectories directories;
    private final ServerPlayerRegistry playerRegistry;

    public ServerStatusService(CloudDirectories directories, ServerPlayerRegistry playerRegistry) {
        this.directories = directories;
        this.playerRegistry = playerRegistry;
    }

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

    private ServerStatus buildStatus(String serverName) {
        // Build a status snapshot from disk config and live player counts.
        TemplateConfig config = loadTemplateConfig(serverName);
        String templateName = readTemplateName(serverName, config);
        int port = readServerPort(serverName);
        ServerPlayerRegistry.PlayerCounts counts = playerRegistry.getCounts(serverName);
        boolean online = port > 0 && isPortOpen("127.0.0.1", port);
        if (counts != null) {
            // Plugin updates are considered authoritative for online state.
            online = true;
        }
        int currentPlayers = counts != null ? counts.getCurrentPlayers() : 0;
        int maxPlayers = counts != null ? counts.getMaxPlayers() : readMaxPlayers(config);
        return new ServerStatus(serverName, templateName, port, online, currentPlayers, maxPlayers);
    }

    private TemplateConfig loadTemplateConfig(String serverName) {
        Path configFile = directories.getTmpServerDir(serverName).resolve("template.yml");
        try {
            return TemplateConfig.loadFrom(configFile);
        } catch (IOException e) {
            // Missing or unreadable template config is tolerated.
            return null;
        }
    }

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

    private int readMaxPlayers(TemplateConfig config) {
        if (config == null) {
            return 0;
        }
        return Math.max(0, config.getMaxPlayers());
    }

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
