package com.duelistic.system;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.duelistic.ui.ConsoleUi;

/**
 * Manages server processes using GNU screen sessions.
 */
public class ScreenServerProcessManager implements ServerProcessManager {
    /**
     * Starts a server in a detached screen session.
     */
    @Override
    public void startServer(String name, List<String> command, Path workingDir) {
        // Start the server in a detached screen session.
        List<String> fullCommand = new ArrayList<>();
        fullCommand.add("screen");
        fullCommand.add("-S");
        fullCommand.add(name);
        fullCommand.add("-dm");
        fullCommand.addAll(command);
        runCommand(fullCommand, workingDir, false);
    }

    /**
     * Stops the screen session for the named server.
     */
    @Override
    public void stopServer(String name) {
        // Stop the screen session by name.
        List<String> fullCommand = new ArrayList<>();
        fullCommand.add("screen");
        fullCommand.add("-S");
        fullCommand.add(name);
        fullCommand.add("-X");
        fullCommand.add("quit");
        runCommand(fullCommand, null, false);
    }

    /**
     * Lists active screen session names that represent servers.
     */
    @Override
    public List<String> listServers() {
        // Parse "screen -ls" output into session names.
        List<String> listCommand = new ArrayList<>();
        listCommand.add("screen");
        listCommand.add("-ls");
        List<String> lines = runCommand(listCommand, null, true);
        if (lines.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> servers = new ArrayList<>();
        for (String line : lines) {
            String trimmed = line.trim();
            int dotIndex = trimmed.indexOf('.');
            if (dotIndex <= 0) {
                continue;
            }
            int end = trimmed.indexOf(' ', dotIndex + 1);
            if (end == -1) {
                end = trimmed.length();
            }
            String name = trimmed.substring(dotIndex + 1, end).trim();
            if (!name.isEmpty()) {
                servers.add(name);
            }
        }
        return Collections.unmodifiableList(servers);
    }

    /**
     * Attaches to a server screen session for interactive control.
     */
    @Override
    public void attachServer(String name) {
        // Attach to a running screen session.
        List<String> attachCommand = new ArrayList<>();
        attachCommand.add("screen");
        attachCommand.add("-r");
        attachCommand.add(name);
        runCommand(attachCommand, null, false);
    }

    /**
     * Executes a screen command and optionally captures stdout lines.
     */
    private List<String> runCommand(List<String> command, Path workingDir, boolean captureOutput) {
        // Helper to run shell commands, optionally capturing stdout.
        ProcessBuilder builder = new ProcessBuilder(command);
        if (workingDir != null) {
            builder.directory(workingDir.toFile());
        }
        if (!captureOutput) {
            builder.inheritIO();
        }
        try {
            Process process = builder.start();
            if (captureOutput) {
                try (InputStream input = process.getInputStream()) {
                    byte[] data = readAllBytes(input);
                    process.waitFor();
                    String output = new String(data, StandardCharsets.UTF_8);
                    List<String> lines = new ArrayList<>();
                    for (String line : output.split("\\r?\\n")) {
                        lines.add(line);
                    }
                    return lines;
                }
            } else {
                process.waitFor();
            }
        } catch (IOException | InterruptedException e) {
            ConsoleUi.error("Screen command failed: " + e.getMessage());
        }
        return Collections.emptyList();
    }

    /**
     * Reads all bytes from an input stream.
     */
    private byte[] readAllBytes(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int read;
        while ((read = input.read(buffer)) != -1) {
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }

}
