package com.duelistic.http;

import com.duelistic.system.ServerPlayerRegistry;
import com.duelistic.system.ServerShutdown;
import com.duelistic.system.ServerStatus;
import com.duelistic.system.ServerStatusService;
import com.duelistic.ui.ConsoleUi;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Minimal local HTTP API for querying cloud state.
 */
public class CloudHttpServer {
    private static final String LOCAL_HOST = "127.0.0.1";

    private final ServerStatusService statusService;
    private final ServerShutdown serverShutdown;
    private final ServerPlayerRegistry playerRegistry;
    private final int port;
    private HttpServer server;
    private ExecutorService executor;

    /**
     * Creates an HTTP server bound to 127.0.0.1.
     */
    public CloudHttpServer(ServerStatusService statusService,
                           ServerShutdown serverShutdown,
                           ServerPlayerRegistry playerRegistry,
                           int port) {
        this.statusService = statusService;
        this.serverShutdown = serverShutdown;
        this.playerRegistry = playerRegistry;
        this.port = port;
    }

    /**
     * Starts the local HTTP server.
     */
    public void start() {
        if (server != null) {
            return;
        }
        try {
            InetSocketAddress address = new InetSocketAddress(InetAddress.getByName(LOCAL_HOST), port);
            server = HttpServer.create(address, 0);
        } catch (IOException e) {
            ConsoleUi.error("HTTP API failed to bind: " + e.getMessage());
            return;
        }
        server.createContext("/api/health", this::handleHealth);
        server.createContext("/api/servers", this::handleServers);
        executor = Executors.newCachedThreadPool();
        server.setExecutor(executor);
        server.start();
        ConsoleUi.success("HTTP API listening on http://" + LOCAL_HOST + ":" + port);
    }

    /**
     * Stops the HTTP server if running.
     */
    public void stop() {
        if (server == null) {
            return;
        }
        server.stop(0);
        server = null;
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
        ConsoleUi.info("HTTP API stopped.");
    }

    private void handleHealth(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405, "{\"error\":\"method_not_allowed\"}");
            return;
        }
        sendJson(exchange, 200, "{\"status\":\"ok\"}");
    }

    private void handleServers(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if ("/api/servers".equals(path) || "/api/servers/".equals(path)) {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, "{\"error\":\"method_not_allowed\"}");
                return;
            }
            try {
                List<ServerStatus> statuses = statusService.listStatuses();
                sendJson(exchange, 200, buildServersPayload(statuses));
            } catch (IOException e) {
                sendJson(exchange, 500, "{\"error\":\"failed_to_list_servers\"}");
            }
            return;
        }
        if (path != null && !"/api/servers".equals(path)) {
            if (path.startsWith("/api/servers/") && path.endsWith("/stop")) {
                handleStopServer(exchange, path);
                return;
            }
            if (path.startsWith("/api/servers/") && path.endsWith("/players")) {
                handleSetCurrentPlayers(exchange, path);
                return;
            }
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, "{\"error\":\"method_not_allowed\"}");
                return;
            }
            String name = path.replaceFirst("^/api/servers/?", "").trim();
            if (name.isEmpty()) {
                sendJson(exchange, 404, "{\"error\":\"not_found\"}");
                return;
            }
            handleSingleServer(exchange, name);
            return;
        }
        sendJson(exchange, 404, "{\"error\":\"not_found\"}");
    }

    private void handleSetCurrentPlayers(HttpExchange exchange, String path) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405, "{\"error\":\"method_not_allowed\"}");
            return;
        }
        String name = path.replaceFirst("^/api/servers/?", "").replaceFirst("/players$", "").trim();
        if (name.isEmpty()) {
            sendJson(exchange, 404, "{\"error\":\"not_found\"}");
            return;
        }
        Integer currentPlayers = readQueryInt(exchange.getRequestURI().getQuery(), "currentPlayers");
        if (currentPlayers == null) {
            sendJson(exchange, 400, "{\"error\":\"missing_current_players\"}");
            return;
        }
        boolean updated = playerRegistry.setCurrentPlayers(name, currentPlayers);
        if (updated) {
            sendJson(exchange, 200, "{\"status\":\"updated\"}");
        } else {
            sendJson(exchange, 404, "{\"error\":\"not_found\"}");
        }
    }

    private void handleStopServer(HttpExchange exchange, String path) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405, "{\"error\":\"method_not_allowed\"}");
            return;
        }
        String name = path.replaceFirst("^/api/servers/?", "").replaceFirst("/stop$", "").trim();
        if (name.isEmpty()) {
            sendJson(exchange, 404, "{\"error\":\"not_found\"}");
            return;
        }
        try {
            boolean stopped = serverShutdown.stop(name);
            if (stopped) {
                sendJson(exchange, 200, "{\"status\":\"stopped\"}");
            } else {
                sendJson(exchange, 404, "{\"error\":\"not_found\"}");
            }
        } catch (IOException e) {
            sendJson(exchange, 500, "{\"error\":\"failed_to_stop_server\"}");
        }
    }

    private static Integer readQueryInt(String query, String key) {
        if (query == null || key == null || key.isEmpty()) {
            return null;
        }
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            String[] parts = pair.split("=", 2);
            if (parts.length == 2 && key.equals(parts[0])) {
                try {
                    return Integer.valueOf(parts[1]);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }
        return null;
    }

    private void handleSingleServer(HttpExchange exchange, String name) throws IOException {
        try {
            List<ServerStatus> statuses = statusService.listStatuses();
            Optional<ServerStatus> match = statuses.stream()
                .filter(status -> status.getName().equalsIgnoreCase(name))
                .findFirst();
            if (match.isPresent()) {
                sendJson(exchange, 200, buildServerPayload(match.get()));
            } else {
                sendJson(exchange, 404, "{\"error\":\"not_found\"}");
            }
        } catch (IOException e) {
            sendJson(exchange, 500, "{\"error\":\"failed_to_list_servers\"}");
        }
    }

    private static void sendJson(HttpExchange exchange, int status, String payload) throws IOException {
        byte[] bytes = payload.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    private static String buildServersPayload(List<ServerStatus> statuses) {
        StringBuilder builder = new StringBuilder();
        builder.append("{\"count\":").append(statuses.size()).append(",\"servers\":[");
        for (int i = 0; i < statuses.size(); i++) {
            if (i > 0) {
                builder.append(",");
            }
            builder.append(buildServerPayload(statuses.get(i)));
        }
        builder.append("]}");
        return builder.toString();
    }

    private static String buildServerPayload(ServerStatus status) {
        StringBuilder builder = new StringBuilder();
        builder.append("{\"name\":\"").append(escape(status.getName())).append("\",");
        builder.append("\"template\":\"").append(escape(status.getTemplate())).append("\",");
        builder.append("\"port\":").append(status.getPort()).append(",");
        builder.append("\"online\":").append(status.isOnline()).append(",");
        builder.append("\"currentPlayers\":").append(status.getCurrentPlayers()).append(",");
        builder.append("\"maxPlayers\":").append(status.getMaxPlayers()).append(",");
        builder.append("\"startedAt\":").append(formatInstant(status.getStartedAt()));
        builder.append("}");
        return builder.toString();
    }

    private static String formatInstant(Instant instant) {
        if (instant == null) {
            return "null";
        }
        return "\"" + escape(instant.toString()) + "\"";
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"':
                    builder.append("\\\"");
                    break;
                case '\\':
                    builder.append("\\\\");
                    break;
                case '\b':
                    builder.append("\\b");
                    break;
                case '\f':
                    builder.append("\\f");
                    break;
                case '\n':
                    builder.append("\\n");
                    break;
                case '\r':
                    builder.append("\\r");
                    break;
                case '\t':
                    builder.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        builder.append(String.format("\\u%04x", (int) c));
                    } else {
                        builder.append(c);
                    }
            }
        }
        return builder.toString();
    }
}
