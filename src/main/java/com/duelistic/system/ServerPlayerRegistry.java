package com.duelistic.system;

import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

public class ServerPlayerRegistry {
    public static class PlayerCounts {
        private final int currentPlayers;
        private final int maxPlayers;

        public PlayerCounts(int currentPlayers, int maxPlayers) {
            this.currentPlayers = currentPlayers;
            this.maxPlayers = maxPlayers;
        }

        public int getCurrentPlayers() {
            return currentPlayers;
        }

        public int getMaxPlayers() {
            return maxPlayers;
        }
    }

    private final Map<String, PlayerCounts> counts = new ConcurrentHashMap<>();

    public void registerServer(String name, int maxPlayers) {
        // Initialize entry for a newly launched server.
        if (name == null || name.trim().isEmpty()) {
            return;
        }
        counts.put(name, new PlayerCounts(0, Math.max(0, maxPlayers)));
    }

    public void setCounts(String name, int currentPlayers, int maxPlayers) {
        // Updates are idempotent and overwrite previous values.
        if (name == null || name.trim().isEmpty()) {
            return;
        }
        int safeCurrent = Math.max(0, currentPlayers);
        int safeMax = Math.max(0, maxPlayers);
        counts.put(name, new PlayerCounts(safeCurrent, safeMax));
    }

    public PlayerCounts getCounts(String name) {
        if (name == null) {
            return null;
        }
        return counts.get(name);
    }

    public void removeServer(String name) {
        if (name == null) {
            return;
        }
        counts.remove(name);
    }

    public void prune(Set<String> activeServers) {
        // Remove entries for servers that no longer exist.
        if (activeServers == null || activeServers.isEmpty()) {
            counts.clear();
            return;
        }
        counts.keySet().retainAll(activeServers);
    }

    public Set<String> getServerNames() {
        // Defensive copy to avoid exposing internal concurrent map keys.
        return new HashSet<>(counts.keySet());
    }
}
