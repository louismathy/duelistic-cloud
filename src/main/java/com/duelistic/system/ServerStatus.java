package com.duelistic.system;

/**
 * Immutable snapshot of a server's current state.
 */
public class ServerStatus {
    // Snapshot of a server's state for API responses.
    private final String name;
    private final String template;
    private final int port;
    private final boolean online;
    private final int currentPlayers;
    private final int maxPlayers;
    private final java.time.Instant startedAt;

    /**
     * Creates a new status snapshot.
     */
    public ServerStatus(String name,
                        String template,
                        int port,
                        boolean online,
                        int currentPlayers,
                        int maxPlayers,
                        java.time.Instant startedAt) {
        this.name = name;
        this.template = template;
        this.port = port;
        this.online = online;
        this.currentPlayers = currentPlayers;
        this.maxPlayers = maxPlayers;
        this.startedAt = startedAt;
    }

    /**
     * Returns the server name.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the template name associated with the server.
     */
    public String getTemplate() {
        return template;
    }

    /**
     * Returns the server port.
     */
    public int getPort() {
        return port;
    }

    /**
     * Returns true if the server appears online.
     */
    public boolean isOnline() {
        return online;
    }

    /**
     * Returns the latest known current player count.
     */
    public int getCurrentPlayers() {
        return currentPlayers;
    }

    /**
     * Returns the latest known max player count.
     */
    public int getMaxPlayers() {
        return maxPlayers;
    }

    /**
     * Returns the server start timestamp when available.
     */
    public java.time.Instant getStartedAt() {
        return startedAt;
    }
}
