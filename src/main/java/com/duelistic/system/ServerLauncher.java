package com.duelistic.system;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.duelistic.ui.ConsoleUi;

public class ServerLauncher {
    private static final int BASE_PORT = 25565;
    private final CloudDirectories directories;
    private final ServerProcessManager processManager;
    private final ServerPlayerRegistry playerRegistry;

    public ServerLauncher(CloudDirectories directories, ServerProcessManager processManager, ServerPlayerRegistry playerRegistry) {
        this.directories = directories;
        this.processManager = processManager;
        this.playerRegistry = playerRegistry;
    }

    public int startAll() throws IOException {
        // Reset tmp servers and recreate from templates.
        directories.deleteTmp();
        directories.ensureTmpExists();
        List<String> templates = directories.listTemplates();
        if (templates.isEmpty()) {
            throw new IOException("No templates found. Use 'template add' or 'setup' first.");
        }

        int created = 0;
        int nextPort = BASE_PORT;
        Set<Integer> usedPorts = new HashSet<>();
        for (String template : templates) {
            TemplateConfig config = TemplateConfig.loadFrom(directories.getTemplateConfigFile(template));
            int count = Math.max(0, config.getServerMin());
            ConsoleUi.info("Starting " + count + " server(s) for template '" + template + "'.");
            for (int i = 1; i <= count; i++) {
                String serverName = template + "-" + i;
                if (Files.exists(directories.getTmpServerDir(serverName))) {
                    continue;
                }
                // Copy template contents and patch ports before starting.
                directories.copyTemplateToServer(template, serverName);
                int port = findNextFreePort(nextPort, usedPorts);
                usedPorts.add(port);
                nextPort = port + 1;
                Path serverDir = directories.getTmpServerDir(serverName);
                updatePorts(serverDir, port);
                Path jarFile = findServerJar(serverDir);
                int ramMb = config.getMaxRamMb();
                if (ramMb <= 0) {
                    throw new IOException("Invalid maxRamMb for template: " + template);
                }
                List<String> command = new ArrayList<>();
                // Build JVM command for the server process.
                command.add("java");
                command.add("-Xms" + ramMb + "M");
                command.add("-Xmx" + ramMb + "M");
                command.add("-jar");
                command.add(jarFile.getFileName().toString());
                processManager.startServer(serverName, command, serverDir);
                playerRegistry.registerServer(serverName, config.getMaxPlayers());
                ConsoleUi.success("Started " + serverName + " on port " + port + " (" + ramMb + "MB RAM)");
                created++;
            }
        }
        return created;
    }

    public String startTemplateServer(String templateName) throws IOException {
        if (!directories.templateExists(templateName)) {
            throw new IOException("Template not found: " + templateName);
        }
        TemplateConfig config = TemplateConfig.loadFrom(directories.getTemplateConfigFile(templateName));
        if (config.getMaxRamMb() <= 0) {
            throw new IOException("Invalid maxRamMb for template: " + templateName);
        }
        // Determine next index and pick a free port.
        List<String> existingServers = directories.listTmpServers();
        Set<Integer> usedPorts = new HashSet<>();
        int maxIndex = 0;
        for (String serverName : existingServers) {
            int port = readServerPort(serverName);
            if (port > 0) {
                usedPorts.add(port);
            }
            int index = parseServerIndex(templateName, serverName);
            if (index > maxIndex) {
                maxIndex = index;
            }
        }
        String serverName = templateName + "-" + (maxIndex + 1);
        while (Files.exists(directories.getTmpServerDir(serverName))) {
            maxIndex++;
            serverName = templateName + "-" + (maxIndex + 1);
        }
        directories.copyTemplateToServer(templateName, serverName);
        int port = findNextFreePort(BASE_PORT, usedPorts);
        updatePorts(directories.getTmpServerDir(serverName), port);
        Path jarFile = findServerJar(directories.getTmpServerDir(serverName));
        List<String> command = new ArrayList<>();
        // Start a single server instance for scale-up.
        command.add("java");
        command.add("-Xms" + config.getMaxRamMb() + "M");
        command.add("-Xmx" + config.getMaxRamMb() + "M");
        command.add("-jar");
        command.add(jarFile.getFileName().toString());
        processManager.startServer(serverName, command, directories.getTmpServerDir(serverName));
        playerRegistry.registerServer(serverName, config.getMaxPlayers());
        ConsoleUi.success("Started " + serverName + " on port " + port + " (" + config.getMaxRamMb() + "MB RAM)");
        return serverName;
    }

    private int findNextFreePort(int startPort, Set<Integer> usedPorts) throws IOException {
        // Scan for a free TCP port, avoiding ones already assigned.
        int port = Math.max(1, startPort);
        while (port <= 65535) {
            if (!usedPorts.contains(port) && isPortFree(port)) {
                return port;
            }
            port++;
        }
        throw new IOException("No free ports available.");
    }

    private boolean isPortFree(int port) {
        try (ServerSocket socket = new ServerSocket(port)) {
            socket.setReuseAddress(true);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private void updatePorts(Path serverDir, int port) throws IOException {
        Path propertiesFile = serverDir.resolve("server.properties");
        if (!Files.exists(propertiesFile)) {
            ConsoleUi.warn("Missing server.properties in " + serverDir.getFileName());
            return;
        }
        // Update server-port and query.port in server.properties.
        List<String> lines = Files.readAllLines(propertiesFile, StandardCharsets.UTF_8);
        List<String> updated = new ArrayList<>(lines.size() + 2);
        boolean replacedQuery = false;
        boolean replacedServer = false;
        for (String line : lines) {
            if (line.startsWith("query.port=")) {
                updated.add("query.port=" + port);
                replacedQuery = true;
            } else if (line.startsWith("server-port=")) {
                updated.add("server-port=" + port);
                replacedServer = true;
            } else {
                updated.add(line);
            }
        }
        if (!replacedQuery) {
            updated.add("query.port=" + port);
        }
        if (!replacedServer) {
            updated.add("server-port=" + port);
        }
        Files.write(propertiesFile, updated, StandardCharsets.UTF_8);
    }

    private Path findServerJar(Path serverDir) throws IOException {
        // Pick the first jar in the directory as the server jar.
        List<Path> jars = new ArrayList<>();
        try (java.util.stream.Stream<Path> stream = Files.list(serverDir)) {
            stream
                .filter(path -> Files.isRegularFile(path))
                .filter(path -> path.getFileName().toString().toLowerCase().endsWith(".jar"))
                .sorted()
                .forEach(jars::add);
        }
        if (jars.isEmpty()) {
            throw new IOException("No server jar found in " + serverDir.getFileName());
        }
        return jars.get(0);
    }

    private int readServerPort(String serverName) {
        // Read server-port from server.properties for existing servers.
        Path propertiesFile = directories.getTmpServerDir(serverName).resolve("server.properties");
        if (!Files.exists(propertiesFile)) {
            return -1;
        }
        java.util.Properties props = new java.util.Properties();
        try (java.io.InputStream input = Files.newInputStream(propertiesFile)) {
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

    private int parseServerIndex(String templateName, String serverName) {
        // Extract numeric suffix from "<template>-<index>".
        String prefix = templateName + "-";
        if (!serverName.startsWith(prefix)) {
            return 0;
        }
        String suffix = serverName.substring(prefix.length());
        try {
            return Integer.parseInt(suffix);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
