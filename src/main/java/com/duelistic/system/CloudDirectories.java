package com.duelistic.system;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Manages filesystem paths and operations for templates and tmp servers.
 */
public class CloudDirectories {
    private final Path baseDir;

    /**
     * Creates a directory helper using the default base location.
     */
    public CloudDirectories() {
        this(defaultBaseDir());
    }

    /**
     * Creates a directory helper using a custom base directory.
     *
     * @param baseDir root for templates and tmp servers.
     */
    public CloudDirectories(Path baseDir) {
        this.baseDir = baseDir;
    }

    /**
     * Ensures the base directory exists on disk.
     */
    public void ensureExists() throws IOException {
        // Create the base system directory if missing.
        Files.createDirectories(baseDir);
    }

    /**
     * Ensures a template directory exists for the given name.
     */
    public void ensureTemplateExists(String name) throws IOException {
        // Create an empty template directory.
        Files.createDirectories(getTemplatesDir().resolve(name));
    }

    /**
     * Copies the default template source into a new template directory.
     */
    public void copyDefaultToTemplate(String name) throws IOException {
        Path sourceDir = getDefaultSourceDir();
        if (!Files.exists(sourceDir)) {
            throw new IOException("Default source directory not found: " + sourceDir.toAbsolutePath());
        }
        Path targetDir = getTemplateDir(name);
        // Copy the default server files into a new template.
        copyDirectoryContents(sourceDir, targetDir);
    }

    /**
     * Copies a template into a tmp server directory.
     */
    public void copyTemplateToServer(String templateName, String serverName) throws IOException {
        Path sourceDir = getTemplateDir(templateName);
        if (!Files.exists(sourceDir)) {
            throw new IOException("Template not found: " + templateName);
        }
        Path targetDir = getTmpServerDir(serverName);
        // Copy template contents into a tmp server directory.
        copyDirectoryContents(sourceDir, targetDir);
    }

    /**
     * Returns all template directory names, sorted.
     */
    public List<String> listTemplates() throws IOException {
        if (!Files.exists(getTemplatesDir())) {
            return Collections.emptyList();
        }
        try (Stream<Path> stream = Files.list(getTemplatesDir())) {
            return stream
                .filter(Files::isDirectory)
                .map(path -> path.getFileName().toString())
                .sorted()
                .collect(Collectors.toList());
        }
    }

    /**
     * Deletes a template directory and its contents.
     */
    public void deleteTemplate(String name) throws IOException {
        // Remove a template directory recursively.
        deleteDirectory(getTemplatesDir().resolve(name));
    }

    /**
     * Returns the configured base directory.
     */
    public Path getBaseDir() {
        return baseDir;
    }

    /**
     * Returns the templates directory path.
     */
    public Path getTemplatesDir() {
        return baseDir.resolve("templates");
    }

    /**
     * Returns the path for a specific template directory.
     */
    public Path getTemplateDir(String name) {
        return getTemplatesDir().resolve(name);
    }

    /**
     * Returns the template configuration file path.
     */
    public Path getTemplateConfigFile(String name) {
        return getTemplateDir(name).resolve("template.yml");
    }

    /**
     * Returns true if the template directory exists.
     */
    public boolean templateExists(String name) {
        return Files.exists(getTemplateDir(name));
    }

    /**
     * Returns the default source directory for templates.
     */
    public Path getDefaultSourceDir() {
        // Default template source is in project "default" directory.
        return Paths.get("default");
    }

    /**
     * Returns the SQL config file path.
     */
    public Path getSqlConfigFile() {
        return baseDir.resolve("sql.yml");
    }

    /**
     * Returns the main config file path.
     */
    public Path getConfigFile() {
        return baseDir.resolve("config.yml");
    }

    /**
     * Returns the tmp directory path.
     */
    public Path getTmpDir() {
        return baseDir.resolve("tmp");
    }

    /**
     * Returns the tmp directory path for a specific server.
     */
    public Path getTmpServerDir(String name) {
        return getTmpDir().resolve(name);
    }

    /**
     * Ensures the tmp directory exists.
     */
    public void ensureTmpExists() throws IOException {
        // Ensure tmp directory exists for generated servers.
        Files.createDirectories(getTmpDir());
    }

    /**
     * Lists tmp server directories, sorted by name.
     */
    public List<String> listTmpServers() throws IOException {
        if (!Files.exists(getTmpDir())) {
            return Collections.emptyList();
        }
        try (Stream<Path> stream = Files.list(getTmpDir())) {
            return stream
                .filter(Files::isDirectory)
                .map(path -> path.getFileName().toString())
                .sorted()
                .collect(Collectors.toList());
        }
    }

    /**
     * Deletes the entire tmp directory and its contents.
     */
    public void deleteTmp() throws IOException {
        // Remove all tmp servers.
        deleteDirectory(getTmpDir());
    }

    /**
     * Deletes a single tmp server directory.
     */
    public void deleteTmpServer(String name) throws IOException {
        // Remove a single tmp server.
        deleteDirectory(getTmpServerDir(name));
    }

    /**
     * Recursively copies the contents of one directory into another.
     */
    private void copyDirectoryContents(Path sourceDir, Path targetDir) throws IOException {
        Files.createDirectories(targetDir);
        try (Stream<Path> stream = Files.walk(sourceDir)) {
            List<Path> paths = stream.collect(Collectors.toList());
            for (Path path : paths) {
                if (path.equals(sourceDir)) {
                    continue;
                }
                Path relative = sourceDir.relativize(path);
                Path destination = targetDir.resolve(relative);
                if (Files.isDirectory(path)) {
                    Files.createDirectories(destination);
                } else {
                    Files.createDirectories(destination.getParent());
                    Files.copy(path, destination, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    /**
     * Computes the default base directory next to the running jar.
     */
    private static Path defaultBaseDir() {
        try {
            Path location = Paths.get(CloudDirectories.class.getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .toURI());
            if (!Files.isDirectory(location)) {
                Path dir = location.getParent();
                if (dir != null) {
                    return dir.resolve("system");
                }
            }
        } catch (URISyntaxException e) {
            // Fall back to working directory when location cannot be resolved.
        }
        return Paths.get("system");
    }

    /**
     * Recursively deletes a directory and its contents.
     */
    private void deleteDirectory(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return;
        }
        // Delete children before the directory itself.
        try (Stream<Path> stream = Files.walk(dir)) {
            List<Path> paths = stream.sorted(Comparator.reverseOrder()).collect(Collectors.toList());
            for (Path path : paths) {
                Files.deleteIfExists(path);
            }
        }
    }
}
