package com.duelistic.system;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CloudDirectories {
    private final Path baseDir;

    public CloudDirectories() {
        this(Paths.get("system"));
    }

    public CloudDirectories(Path baseDir) {
        this.baseDir = baseDir;
    }

    public void ensureExists() throws IOException {
        // Create the base system directory if missing.
        Files.createDirectories(baseDir);
    }

    public void ensureTemplateExists(String name) throws IOException {
        // Create an empty template directory.
        Files.createDirectories(getTemplatesDir().resolve(name));
    }

    public void copyDefaultToTemplate(String name) throws IOException {
        Path sourceDir = getDefaultSourceDir();
        if (!Files.exists(sourceDir)) {
            throw new IOException("Default source directory not found: " + sourceDir.toAbsolutePath());
        }
        Path targetDir = getTemplateDir(name);
        // Copy the default server files into a new template.
        copyDirectoryContents(sourceDir, targetDir);
    }

    public void copyTemplateToServer(String templateName, String serverName) throws IOException {
        Path sourceDir = getTemplateDir(templateName);
        if (!Files.exists(sourceDir)) {
            throw new IOException("Template not found: " + templateName);
        }
        Path targetDir = getTmpServerDir(serverName);
        // Copy template contents into a tmp server directory.
        copyDirectoryContents(sourceDir, targetDir);
    }

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

    public void deleteTemplate(String name) throws IOException {
        // Remove a template directory recursively.
        deleteDirectory(getTemplatesDir().resolve(name));
    }

    public Path getBaseDir() {
        return baseDir;
    }

    public Path getTemplatesDir() {
        return baseDir.resolve("templates");
    }

    public Path getTemplateDir(String name) {
        return getTemplatesDir().resolve(name);
    }

    public Path getTemplateConfigFile(String name) {
        return getTemplateDir(name).resolve("template.yml");
    }

    public boolean templateExists(String name) {
        return Files.exists(getTemplateDir(name));
    }

    public Path getDefaultSourceDir() {
        // Default template source is in project "default" directory.
        return Paths.get("default");
    }

    public Path getTmpDir() {
        return baseDir.resolve("tmp");
    }

    public Path getTmpServerDir(String name) {
        return getTmpDir().resolve(name);
    }

    public void ensureTmpExists() throws IOException {
        // Ensure tmp directory exists for generated servers.
        Files.createDirectories(getTmpDir());
    }

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

    public void deleteTmp() throws IOException {
        // Remove all tmp servers.
        deleteDirectory(getTmpDir());
    }

    public void deleteTmpServer(String name) throws IOException {
        // Remove a single tmp server.
        deleteDirectory(getTmpServerDir(name));
    }

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
