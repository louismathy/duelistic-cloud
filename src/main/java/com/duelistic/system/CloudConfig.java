package com.duelistic.system;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;

/**
 * Loads and stores application-level configuration from config.yml.
 */
public class CloudConfig {
    private static final long DEFAULT_AUTO_RENEW_INTERVAL_MS = 5000;
    private static final long DEFAULT_TEMPLATE_SYNC_INTERVAL_MS = 15_000;
    private static final long DEFAULT_BAN_CLEANUP_INTERVAL_MS = 60_000;
    private static final long DEFAULT_SERVER_SYNC_INTERVAL_MS = 10_000;
    private static final long DEFAULT_DASHBOARD_INTERVAL_MS = 15_000;
    private static final long DEFAULT_METRICS_INTERVAL_MS = 30 * 60_000;
    private static final boolean DEFAULT_ONLY_START_SERVER_IF_FREE_RAM = true;
    private static final boolean DEFAULT_BASED_ON_OVERALL_SYSTEM_MEMORY = false;
    private static final int DEFAULT_VIRTUAL_RAM_LIMIT_MB = 4096;
    private static final boolean DEFAULT_HTTP_API_ENABLED = true;
    private static final int DEFAULT_HTTP_API_PORT = 8085;

    private final long autoRenewIntervalMs;
    private final long templateSyncIntervalMs;
    private final long banCleanupIntervalMs;
    private final long serverSyncIntervalMs;
    private final long dashboardMetricsIntervalMs;
    private final long onlineMetricsIntervalMs;
    private final boolean onlyStartServerIfFreeRam;
    private final boolean basedOnOverallSystemMemory;
    private final int virtualRamLimitMb;
    private final boolean httpApiEnabled;
    private final int httpApiPort;

    private CloudConfig(long autoRenewIntervalMs,
                        long templateSyncIntervalMs,
                        long banCleanupIntervalMs,
                        long serverSyncIntervalMs,
                        long dashboardMetricsIntervalMs,
                        long onlineMetricsIntervalMs,
                        boolean onlyStartServerIfFreeRam,
                        boolean basedOnOverallSystemMemory,
                        int virtualRamLimitMb,
                        boolean httpApiEnabled,
                        int httpApiPort) {
        this.autoRenewIntervalMs = autoRenewIntervalMs;
        this.templateSyncIntervalMs = templateSyncIntervalMs;
        this.banCleanupIntervalMs = banCleanupIntervalMs;
        this.serverSyncIntervalMs = serverSyncIntervalMs;
        this.dashboardMetricsIntervalMs = dashboardMetricsIntervalMs;
        this.onlineMetricsIntervalMs = onlineMetricsIntervalMs;
        this.onlyStartServerIfFreeRam = onlyStartServerIfFreeRam;
        this.basedOnOverallSystemMemory = basedOnOverallSystemMemory;
        this.virtualRamLimitMb = virtualRamLimitMb;
        this.httpApiEnabled = httpApiEnabled;
        this.httpApiPort = httpApiPort;
    }

    /**
     * Loads config from YAML, falling back to defaults when missing.
     */
    public static CloudConfig loadFrom(Path configFile) {
        if (configFile == null || !Files.exists(configFile)) {
            return defaults();
        }
        Yaml yaml = new Yaml();
        try {
            Map<String, Object> data = yaml.load(Files.newBufferedReader(configFile, StandardCharsets.UTF_8));
            if (data == null) {
                return defaults();
            }
            long autoRenewIntervalMs = readLong(data, "autoRenewIntervalMs", DEFAULT_AUTO_RENEW_INTERVAL_MS);
            long templateSyncIntervalMs = readLong(data, "templateSyncIntervalMs", DEFAULT_TEMPLATE_SYNC_INTERVAL_MS);
            long banCleanupIntervalMs = readLong(data, "banCleanupIntervalMs", DEFAULT_BAN_CLEANUP_INTERVAL_MS);
            long serverSyncIntervalMs = readLong(data, "serverSyncIntervalMs", DEFAULT_SERVER_SYNC_INTERVAL_MS);
            long dashboardMetricsIntervalMs = readLong(data, "dashboardMetricsIntervalMs", DEFAULT_DASHBOARD_INTERVAL_MS);
            long onlineMetricsIntervalMs = readLong(data, "onlineMetricsIntervalMs", DEFAULT_METRICS_INTERVAL_MS);
            boolean onlyStartServerIfFreeRam = readBoolean(data, "onlyStartServerIfFreeRam", DEFAULT_ONLY_START_SERVER_IF_FREE_RAM);
            boolean basedOnOverallSystemMemory = readBoolean(data, "basedOnOverallSystemMemory", DEFAULT_BASED_ON_OVERALL_SYSTEM_MEMORY);
            int virtualRamLimitMb = readInt(data, "virtualRamLimitMb", DEFAULT_VIRTUAL_RAM_LIMIT_MB);
            boolean httpApiEnabled = readBoolean(data, "httpApiEnabled", DEFAULT_HTTP_API_ENABLED);
            int httpApiPort = readInt(data, "httpApiPort", DEFAULT_HTTP_API_PORT);
            return new CloudConfig(normalizeInterval(autoRenewIntervalMs, DEFAULT_AUTO_RENEW_INTERVAL_MS),
                normalizeInterval(templateSyncIntervalMs, DEFAULT_TEMPLATE_SYNC_INTERVAL_MS),
                normalizeInterval(banCleanupIntervalMs, DEFAULT_BAN_CLEANUP_INTERVAL_MS),
                normalizeInterval(serverSyncIntervalMs, DEFAULT_SERVER_SYNC_INTERVAL_MS),
                normalizeInterval(dashboardMetricsIntervalMs, DEFAULT_DASHBOARD_INTERVAL_MS),
                normalizeInterval(onlineMetricsIntervalMs, DEFAULT_METRICS_INTERVAL_MS),
                onlyStartServerIfFreeRam,
                basedOnOverallSystemMemory,
                virtualRamLimitMb,
                httpApiEnabled,
                httpApiPort);
        } catch (IOException e) {
            return defaults();
        }
    }

    /**
     * Writes a default config file if one does not exist.
     */
    public static void writeDefaultIfMissing(Path configFile) {
        if (configFile == null || Files.exists(configFile)) {
            return;
        }
        try {
            Files.createDirectories(configFile.getParent());
            StringBuilder builder = new StringBuilder();
            builder.append("# Duelistic Cloud configuration\n");
            builder.append("autoRenewIntervalMs: ").append(DEFAULT_AUTO_RENEW_INTERVAL_MS).append("\n");
            builder.append("templateSyncIntervalMs: ").append(DEFAULT_TEMPLATE_SYNC_INTERVAL_MS).append("\n");
            builder.append("banCleanupIntervalMs: ").append(DEFAULT_BAN_CLEANUP_INTERVAL_MS).append("\n");
            builder.append("serverSyncIntervalMs: ").append(DEFAULT_SERVER_SYNC_INTERVAL_MS).append("\n");
            builder.append("dashboardMetricsIntervalMs: ").append(DEFAULT_DASHBOARD_INTERVAL_MS).append("\n");
            builder.append("onlineMetricsIntervalMs: ").append(DEFAULT_METRICS_INTERVAL_MS).append("\n");
            builder.append("onlyStartServerIfFreeRam: ").append(DEFAULT_ONLY_START_SERVER_IF_FREE_RAM).append("\n");
            builder.append("basedOnOverallSystemMemory: ").append(DEFAULT_BASED_ON_OVERALL_SYSTEM_MEMORY).append("\n");
            builder.append("virtualRamLimitMb: ").append(DEFAULT_VIRTUAL_RAM_LIMIT_MB).append("\n");
            builder.append("httpApiEnabled: ").append(DEFAULT_HTTP_API_ENABLED).append("\n");
            builder.append("httpApiPort: ").append(DEFAULT_HTTP_API_PORT).append("\n");
            Files.write(configFile, builder.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            // If default config can't be written, stay silent and continue.
        }
    }

    private static CloudConfig defaults() {
        return new CloudConfig(DEFAULT_AUTO_RENEW_INTERVAL_MS,
            DEFAULT_TEMPLATE_SYNC_INTERVAL_MS,
            DEFAULT_BAN_CLEANUP_INTERVAL_MS,
            DEFAULT_SERVER_SYNC_INTERVAL_MS,
            DEFAULT_DASHBOARD_INTERVAL_MS,
            DEFAULT_METRICS_INTERVAL_MS,
            DEFAULT_ONLY_START_SERVER_IF_FREE_RAM,
            DEFAULT_BASED_ON_OVERALL_SYSTEM_MEMORY,
            DEFAULT_VIRTUAL_RAM_LIMIT_MB,
            DEFAULT_HTTP_API_ENABLED,
            DEFAULT_HTTP_API_PORT);
    }

    private static long normalizeInterval(long value, long fallback) {
        if (value <= 0) {
            return fallback;
        }
        return value;
    }

    private static int readInt(Map<String, Object> data, String key, int fallback) {
        Object value = data.get(key);
        if (value == null) {
            return fallback;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static long readLong(Map<String, Object> data, String key, long fallback) {
        Object value = data.get(key);
        if (value == null) {
            return fallback;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static boolean readBoolean(Map<String, Object> data, String key, boolean fallback) {
        Object value = data.get(key);
        if (value == null) {
            return fallback;
        }

        if (value instanceof Boolean) {
            return (Boolean) value;
        }

        return Boolean.parseBoolean(value.toString());
    }


    /**
     * Returns the auto-renew check interval.
     */
    public long getAutoRenewIntervalMs() {
        return autoRenewIntervalMs;
    }

    /**
     * Returns the template SQL sync interval.
     */
    public long getTemplateSyncIntervalMs() {
        return templateSyncIntervalMs;
    }

    /**
     * Returns the ban cleanup interval.
     */
    public long getBanCleanupIntervalMs() {
        return banCleanupIntervalMs;
    }

    /**
     * Returns the server SQL sync interval.
     */
    public long getServerSyncIntervalMs() {
        return serverSyncIntervalMs;
    }

    /**
     * Returns the dashboard metrics interval.
     */
    public long getDashboardMetricsIntervalMs() {
        return dashboardMetricsIntervalMs;
    }

    /**
     * Returns the online metrics interval.
     */
    public long getOnlineMetricsIntervalMs() {
        return onlineMetricsIntervalMs;
    }

    /**
     * returns if servers should only start if enough memory is free
     */
    public boolean getOnlyStartServerIfFreeRam() {
        return onlyStartServerIfFreeRam;
    }

    /**
     * Returns whether decisions should be based on overall system memory.
     */
    public boolean isBasedOnOverallSystemMemory() {
        return basedOnOverallSystemMemory;
    }

    /**
     * Returns the virtual RAM limit in MB.
     */
    public int getVirtualRamLimitMb() {
        return virtualRamLimitMb;
    }

    /**
     * Returns whether the local HTTP API should be started.
     */
    public boolean isHttpApiEnabled() {
        return httpApiEnabled;
    }

    /**
     * Returns the local HTTP API port.
     */
    public int getHttpApiPort() {
        return httpApiPort;
    }
}
