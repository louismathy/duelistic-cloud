package com.duelistic.system;

import java.nio.file.Path;
import java.util.List;

/**
 * Abstraction for starting and controlling server processes.
 */
public interface ServerProcessManager {
    /**
     * Starts a server process in the given working directory.
     */
    void startServer(String name, List<String> command, Path workingDir);

    /**
     * Stops a server process by name.
     */
    void stopServer(String name);

    /**
     * Lists running server process names.
     */
    List<String> listServers();

    /**
     * Attaches to an interactive server process.
     */
    void attachServer(String name);
}
