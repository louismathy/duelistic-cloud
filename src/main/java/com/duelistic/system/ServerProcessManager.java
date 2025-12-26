package com.duelistic.system;

import java.nio.file.Path;
import java.util.List;

public interface ServerProcessManager {
    // Start a server process in the given working directory.
    void startServer(String name, List<String> command, Path workingDir);

    // Stop a server process by name.
    void stopServer(String name);

    // List running server process names.
    List<String> listServers();

    // Attach to an interactive server process.
    void attachServer(String name);
}
