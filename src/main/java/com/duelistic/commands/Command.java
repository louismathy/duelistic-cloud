package com.duelistic.commands;

/**
 * Represents a CLI command that can be invoked by name.
 */
public interface Command {
    /**
     * Returns the command name used for lookup.
     */
    String getName();
    String getUsage();

    /**
     * Executes the command with the provided arguments.
     */
    void execute(String[] args);
}
