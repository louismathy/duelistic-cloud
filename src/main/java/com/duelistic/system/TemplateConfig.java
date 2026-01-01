package com.duelistic.system;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

/**
 * Represents configuration for a server template stored in YAML.
 */
public class TemplateConfig {
    private final String templateName;
    private final int maxRamMb;
    private final int maxPlayers;
    private final int serverMin;
    private final int serverMax;

    /**
     * Creates a template configuration.
     */
    public TemplateConfig(String templateName, int maxRamMb, int maxPlayers, int serverMin, int serverMax) {
        this.templateName = templateName;
        this.maxRamMb = maxRamMb;
        this.maxPlayers = maxPlayers;
        this.serverMin = serverMin;
        this.serverMax = serverMax;
    }

    /**
     * Returns the template name.
     */
    public String getTemplateName() {
        return templateName;
    }

    /**
     * Returns the max RAM in MB.
     */
    public int getMaxRamMb() {
        return maxRamMb;
    }

    /**
     * Returns the max player capacity.
     */
    public int getMaxPlayers() {
        return maxPlayers;
    }

    /**
     * Returns the minimum number of servers to keep running.
     */
    public int getServerMin() {
        return serverMin;
    }

    /**
     * Returns the maximum number of servers to allow.
     */
    public int getServerMax() {
        return serverMax;
    }

    /**
     * Writes the config to a YAML file on disk.
     */
    public void writeTo(Path configFile) throws IOException {
        // Persist template settings in YAML format.
        Files.createDirectories(configFile.getParent());
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Yaml yaml = new Yaml(options);
        try (Writer writer = Files.newBufferedWriter(configFile, StandardCharsets.UTF_8)) {
            yaml.dump(toMap(), writer);
        }
    }

    /**
     * Loads template config from a YAML file.
     */
    public static TemplateConfig loadFrom(Path configFile) throws IOException {
        // Load template config from YAML and validate required keys.
        if (!Files.exists(configFile)) {
            throw new IOException("Missing template config: " + configFile.toAbsolutePath());
        }
        Yaml yaml = new Yaml();
        Map<String, Object> data = yaml.load(Files.newBufferedReader(configFile, StandardCharsets.UTF_8));
        if (data == null) {
            throw new IOException("Empty template config: " + configFile.toAbsolutePath());
        }
        int maxRamMb = readInt(data, "maxRamMb");
        int maxPlayers = readInt(data, "maxPlayers");
        int serverMin = readInt(data, "serverMin");
        int serverMax = readInt(data, "serverMax");
        String templateName = readString(data, "templateName");
        return new TemplateConfig(templateName, maxRamMb, maxPlayers, serverMin, serverMax);
    }

    /**
     * Serializes the config into a YAML-friendly map.
     */
    private Map<String, Object> toMap() {
        // Serialize only non-empty template name.
        Map<String, Object> data = new LinkedHashMap<>();
        if (templateName != null && !templateName.isEmpty()) {
            data.put("templateName", templateName);
        }
        data.put("maxRamMb", maxRamMb);
        data.put("maxPlayers", maxPlayers);
        data.put("serverMin", serverMin);
        data.put("serverMax", serverMax);
        return data;
    }

    /**
     * Reads a required integer field from the YAML map.
     */
    private static int readInt(Map<String, Object> data, String key) throws IOException {
        Object value = data.get(key);
        if (value == null) {
            throw new IOException("Missing '" + key + "' in template config.");
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            throw new IOException("Invalid '" + key + "' value: " + value);
        }
    }

    /**
     * Reads an optional string field from the YAML map.
     */
    private static String readString(Map<String, Object> data, String key) {
        // Strings are optional and trimmed.
        Object value = data.get(key);
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }
}
