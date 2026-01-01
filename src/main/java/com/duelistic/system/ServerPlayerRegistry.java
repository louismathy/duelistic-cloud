package com.duelistic.system;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores live player counts and timestamps for running servers.
 */
public class ServerPlayerRegistry {
    /**
     * Immutable snapshot of current and max player counts.
     */
    public static class PlayerCounts {
        private final int currentPlayers;
        private final int maxPlayers;

        /**
         * Creates a new count snapshot.
         */
        public PlayerCounts(int currentPlayers, int maxPlayers) {
            this.currentPlayers = currentPlayers;
            this.maxPlayers = maxPlayers;
        }

        /**
         * Returns the current player count.
         */
        public int getCurrentPlayers() {
            return currentPlayers;
        }

        /**
         * Returns the maximum player capacity.
         */
        public int getMaxPlayers() {
            return maxPlayers;
        }
    }

    private final Map<String, PlayerCounts> counts = new ConcurrentHashMap<>();
    private final Map<String, Long> startedAt = new ConcurrentHashMap<>();
    private final Map<String, Long> lastUpdatedAt = new ConcurrentHashMap<>();
    private final Map<String, String> displayNames = new ConcurrentHashMap<>();

    /**
     * Registers a newly started server and initializes its counts.
     */
    public void registerServer(String name, int maxPlayers) {
        // Initialize entry for a newly launched server.
        if (name == null || name.trim().isEmpty()) {
            return;
        }
        counts.put(name, new PlayerCounts(0, Math.max(0, maxPlayers)));
        displayNames.putIfAbsent(name, name);
        startedAt.putIfAbsent(name, Instant.now().toEpochMilli());
        lastUpdatedAt.put(name, Instant.now().toEpochMilli());
    }

    /**
     * Updates the player counts reported by a server.
     */
    public void setCounts(String name, int currentPlayers, int maxPlayers) {
        setCounts(name, name, currentPlayers, maxPlayers);
    }

    /**
     * Updates the player counts with an optional display name.
     */
    public void setCounts(String serverId, String displayName, int currentPlayers, int maxPlayers) {
        // Updates are idempotent and overwrite previous values.
        if (serverId == null || serverId.trim().isEmpty()) {
            return;
        }
        int safeCurrent = Math.max(0, currentPlayers);
        int safeMax = Math.max(0, maxPlayers);
        String key = serverId.trim();
        counts.put(key, new PlayerCounts(safeCurrent, safeMax));
        if (displayName != null && !displayName.trim().isEmpty()) {
            displayNames.put(key, displayName.trim());
        }
        startedAt.putIfAbsent(key, Instant.now().toEpochMilli());
        lastUpdatedAt.put(key, Instant.now().toEpochMilli());
    }

    /**
     * Updates only the current player count for an existing server.
     *
     * @return true if the server exists and was updated.
     */
    public boolean setCurrentPlayers(String name, int currentPlayers) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        String key = name.trim();
        PlayerCounts existing = counts.get(key);
        if (existing == null) {
            return false;
        }
        String displayName = displayNames.get(key);
        setCounts(key, displayName, currentPlayers, existing.getMaxPlayers());
        return true;
    }

    /**
     * Returns the latest counts for a server.
     */
    public PlayerCounts getCounts(String name) {
        if (name == null) {
            return null;
        }
        return counts.get(name);
    }

    /**
     * Removes all tracking data for a server.
     */
    public void removeServer(String name) {
        if (name == null) {
            return;
        }
        counts.remove(name);
        startedAt.remove(name);
        lastUpdatedAt.remove(name);
        displayNames.remove(name);
    }

    /**
     * Removes any servers not present in the provided active set.
     */
    public void prune(Set<String> activeServers) {
        // Remove entries for servers that no longer exist.
        if (activeServers == null || activeServers.isEmpty()) {
            counts.clear();
            startedAt.clear();
            lastUpdatedAt.clear();
            displayNames.clear();
            return;
        }
        counts.keySet().retainAll(activeServers);
        startedAt.keySet().retainAll(activeServers);
        lastUpdatedAt.keySet().retainAll(activeServers);
        displayNames.keySet().retainAll(activeServers);
    }

    /**
     * Returns a copy of all known server names.
     */
    public Set<String> getServerNames() {
        // Defensive copy to avoid exposing internal concurrent map keys.
        return new HashSet<>(counts.keySet());
    }

    /**
     * Returns the timestamp when the server was first seen.
     */
    public Instant getStartedAt(String name) {
        if (name == null) {
            return null;
        }
        Long value = startedAt.get(name);
        return value == null ? null : Instant.ofEpochMilli(value);
    }

    /**
     * Returns the timestamp of the last counts update.
     */
    public Instant getLastUpdatedAt(String name) {
        if (name == null) {
            return null;
        }
        Long value = lastUpdatedAt.get(name);
        return value == null ? null : Instant.ofEpochMilli(value);
    }

    /**
     * Returns the display name for a server if provided.
     */
    public String getDisplayName(String name) {
        if (name == null) {
            return null;
        }
        return displayNames.get(name);
    }
}
