package com.duelistic.system;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;

/**
 * Represents SQL configuration loaded from system/sql.yml.
 */
public class SqlConfig {
    private final boolean enabled;
    private final String url;
    private final String username;
    private final String password;
    private final String driver;
    private final String table;

    /**
     * Creates a config instance from parsed values.
     */
    private SqlConfig(boolean enabled,
                      String url,
                      String username,
                      String password,
                      String driver,
                      String table) {
        this.enabled = enabled;
        this.url = url;
        this.username = username;
        this.password = password;
        this.driver = driver;
        this.table = table;
    }

    /**
     * Returns a disabled config instance with defaults.
     */
    public static SqlConfig disabled() {
        return new SqlConfig(false, null, null, null, null, "online_player_minutes");
    }

    /**
     * Loads config from YAML, falling back to disabled when missing.
     */
    public static SqlConfig loadFrom(Path configFile) {
        if (configFile == null || !Files.exists(configFile)) {
            return disabled();
        }
        Yaml yaml = new Yaml();
        try {
            Map<String, Object> data = yaml.load(Files.newBufferedReader(configFile, StandardCharsets.UTF_8));
            if (data == null) {
                return disabled();
            }
            boolean enabled = readBoolean(data, "enabled", false);
            String url = readString(data, "url", null);
            String username = readString(data, "username", null);
            String password = readString(data, "password", null);
            String driver = readString(data, "driver", null);
            String table = readString(data, "table", "online_player_minutes");
            return new SqlConfig(enabled, url, username, password, driver, table);
        } catch (IOException e) {
            return disabled();
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
            builder.append("enabled: false\n");
            builder.append("url: \"jdbc:mysql://localhost:3306/duelistic\"\n");
            builder.append("username: \"duelistic_user\"\n");
            builder.append("password: \"change_me\"\n");
            builder.append("driver: \"com.mysql.cj.jdbc.Driver\"\n");
            builder.append("table: \"online_player_minutes\"\n");
            Files.write(configFile, builder.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            // If default config can't be written, stay silent and continue.
        }
    }

    /**
     * Reads a boolean value from the YAML map.
     */
    private static boolean readBoolean(Map<String, Object> data, String key, boolean fallback) {
        Object value = data.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value != null) {
            return Boolean.parseBoolean(value.toString());
        }
        return fallback;
    }

    /**
     * Reads a string value from the YAML map.
     */
    private static String readString(Map<String, Object> data, String key, String fallback) {
        Object value = data.get(key);
        if (value == null) {
            return fallback;
        }
        String text = value.toString().trim();
        return text.isEmpty() ? fallback : text;
    }

    /**
     * Returns whether SQL integration is enabled.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Returns the JDBC URL.
     */
    public String getUrl() {
        return url;
    }

    /**
     * Returns the SQL username.
     */
    public String getUsername() {
        return username;
    }

    /**
     * Returns the SQL password.
     */
    public String getPassword() {
        return password;
    }

    /**
     * Returns the optional JDBC driver class name.
     */
    public String getDriver() {
        return driver;
    }

    /**
     * Returns the table name for metrics storage.
     */
    public String getTable() {
        return table;
    }
}
