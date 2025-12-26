package com.duelistic.http;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.duelistic.system.CloudDirectories;
import com.duelistic.system.ServerLauncher;
import com.duelistic.system.ServerPlayerRegistry;
import com.duelistic.system.ServerShutdown;
import com.duelistic.system.ServerStatus;
import com.duelistic.system.ServerStatusService;
import com.duelistic.ui.ConsoleUi;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

public class CloudHttpServer {
    // Core services are injected to keep HTTP handlers thin and focused.
    private final CloudDirectories directories;
    private final ServerLauncher launcher;
    private final ServerShutdown shutdown;
    private final ServerStatusService statusService;
    private final ServerPlayerRegistry playerRegistry;
    private HttpServer server;
    private ExecutorService executor;

    public CloudHttpServer(CloudDirectories directories,
                           ServerLauncher launcher,
                           ServerShutdown shutdown,
                           ServerStatusService statusService,
                           ServerPlayerRegistry playerRegistry) {
        this.directories = directories;
        this.launcher = launcher;
        this.shutdown = shutdown;
        this.statusService = statusService;
        this.playerRegistry = playerRegistry;
    }

    public void start(String host, int port) throws IOException {
        // Lightweight HTTP server for local admin and plugin updates.
        server = HttpServer.create(new InetSocketAddress(host, port), 0);
        executor = Executors.newCachedThreadPool();
        server.setExecutor(executor);
        server.createContext("/health", this::handleHealth);
        server.createContext("/templates", this::handleTemplates);
        server.createContext("/servers", this::handleServers);
        server.createContext("/start", this::handleStart);
        server.createContext("/stop", this::handleStop);
        server.start();
        ConsoleUi.info("HTTP server started on http://" + host + ":" + port);
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    private void handleHealth(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendMethodNotAllowed(exchange);
            return;
        }
        // Simple health response for readiness checks.
        sendJson(exchange, 200, "{\"status\":\"ok\"}");
    }

    private void handleTemplates(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendMethodNotAllowed(exchange);
            return;
        }
        // List template names for admin UIs.
        List<String> templates = directories.listTemplates();
        sendJson(exchange, 200, "{\"templates\":" + toJsonArray(templates) + "}");
    }

    private void handleServers(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        if ("POST".equalsIgnoreCase(method)) {
            handleServerUpdate(exchange);
            return;
        }
        if (!"GET".equalsIgnoreCase(method)) {
            sendMethodNotAllowed(exchange);
            return;
        }
        // Return the merged view of servers and current counts.
        List<ServerStatus> servers = statusService.listStatuses();
        sendJson(exchange, 200, "{\"servers\":" + toJsonServers(servers) + "}");
    }

    private void handleStart(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendMethodNotAllowed(exchange);
            return;
        }
        try {
            // Start all configured template servers.
            int created = launcher.startAll();
            ConsoleUi.success("HTTP start request created " + created + " servers.");
            sendJson(exchange, 200, "{\"created\":" + created + "}");
        } catch (IOException e) {
            ConsoleUi.error("HTTP start request failed: " + e.getMessage());
            sendJson(exchange, 500, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
        }
    }

    private void handleStop(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendMethodNotAllowed(exchange);
            return;
        }
        try {
            // Stop all running servers managed by the cloud.
            int stopped = shutdown.stopAll();
            ConsoleUi.success("HTTP stop request stopped " + stopped + " servers.");
            sendJson(exchange, 200, "{\"stopped\":" + stopped + "}");
        } catch (IOException e) {
            ConsoleUi.error("HTTP stop request failed: " + e.getMessage());
            sendJson(exchange, 500, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
        }
    }

    private void sendJson(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        Headers headers = exchange.getResponseHeaders();
        headers.set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    private void sendMethodNotAllowed(HttpExchange exchange) throws IOException {
        sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
    }

    private String toJsonArray(List<String> values) {
        StringBuilder builder = new StringBuilder();
        builder.append('[');
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append('\"').append(escapeJson(values.get(i))).append('\"');
        }
        builder.append(']');
        return builder.toString();
    }

    private String toJsonServers(List<ServerStatus> servers) {
        StringBuilder builder = new StringBuilder();
        builder.append('[');
        for (int i = 0; i < servers.size(); i++) {
            if (i > 0) {
                builder.append(',');
            }
            ServerStatus status = servers.get(i);
            // Inline JSON to avoid extra dependencies.
            builder.append('{');
            builder.append("\"name\":\"").append(escapeJson(status.getName())).append("\",");
            builder.append("\"template\":\"").append(escapeJson(status.getTemplate())).append("\",");
            builder.append("\"port\":").append(status.getPort()).append(',');
            builder.append("\"online\":").append(status.isOnline());
            builder.append(',');
            builder.append("\"currentPlayers\":").append(status.getCurrentPlayers()).append(',');
            builder.append("\"maxPlayers\":").append(status.getMaxPlayers());
            builder.append('}');
        }
        builder.append(']');
        return builder.toString();
    }

    private void handleServerUpdate(HttpExchange exchange) throws IOException {
        // Plugin posts JSON: {name, currentPlayers, maxPlayers}.
        String body = readRequestBody(exchange).trim();
        String name = readJsonString(body, "name");
        Integer currentPlayers = readJsonInt(body, "currentPlayers");
        Integer maxPlayers = readJsonInt(body, "maxPlayers");
        if (name == null || name.trim().isEmpty()) {
            sendJson(exchange, 400, "{\"error\":\"Missing name\"}");
            return;
        }
        if (currentPlayers == null || maxPlayers == null) {
            sendJson(exchange, 400, "{\"error\":\"Missing player counts\"}");
            return;
        }
        playerRegistry.setCounts(name.trim(), currentPlayers, maxPlayers);
        sendJson(exchange, 200, "{\"ok\":true}");
    }

    private String readJsonString(String body, String key) {
        String value = readJsonValue(body, key);
        if (value == null) {
            return null;
        }
        if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
            value = value.substring(1, value.length() - 1);
        }
        return value.replace("\\\"", "\"").replace("\\\\", "\\");
    }

    private Integer readJsonInt(String body, String key) {
        String value = readJsonValue(body, key);
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(value.replace("\"", "").trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String readJsonValue(String body, String key) {
        // Minimal JSON parsing for a flat object without nested structures.
        String quotedKey = "\"" + key + "\"";
        int index = body.indexOf(quotedKey);
        if (index == -1) {
            return null;
        }
        int colon = body.indexOf(':', index + quotedKey.length());
        if (colon == -1) {
            return null;
        }
        int start = colon + 1;
        int end = start;
        boolean inQuotes = false;
        while (end < body.length()) {
            char ch = body.charAt(end);
            if (ch == '"' && (end == start || body.charAt(end - 1) != '\\')) {
                inQuotes = !inQuotes;
            } else if (!inQuotes && (ch == ',' || ch == '}')) {
                break;
            }
            end++;
        }
        return body.substring(start, end).trim();
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        // Minimal JSON escaping for response strings.
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String readRequestBody(HttpExchange exchange) throws IOException {
        // Read the entire request body as UTF-8.
        try (java.io.InputStream input = exchange.getRequestBody()) {
            java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        }
    }
}
