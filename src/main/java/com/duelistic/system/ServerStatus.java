package com.duelistic.system;

public class ServerStatus {
    // Snapshot of a server's state for API responses.
    private final String name;
    private final String template;
    private final int port;
    private final boolean online;
    private final int currentPlayers;
    private final int maxPlayers;

    public ServerStatus(String name, String template, int port, boolean online, int currentPlayers, int maxPlayers) {
        this.name = name;
        this.template = template;
        this.port = port;
        this.online = online;
        this.currentPlayers = currentPlayers;
        this.maxPlayers = maxPlayers;
    }

    public String getName() {
        return name;
    }

    public String getTemplate() {
        return template;
    }

    public int getPort() {
        return port;
    }

    public boolean isOnline() {
        return online;
    }

    public int getCurrentPlayers() {
        return currentPlayers;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }
}
