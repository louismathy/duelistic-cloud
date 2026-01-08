package com.duelistic.commands;

import com.duelistic.ui.ConsoleUi;

/**
 * Lists available CLI commands.
 */
public class HelpCommand implements Command {
    private final CommandRegistry registry;

    /**
     * Creates the help command with a command registry.
     */
    public HelpCommand(CommandRegistry registry) {
        this.registry = registry;
    }

    /**
     * Returns the CLI command name.
     */
    @Override
    public String getName() {
        return "help";
    }

    @Override
    public String getUsage() {
        return "help";
    }

    /**
     * Prints command names to the console.
     */
    @Override
    public void execute(String[] args) {
        ConsoleUi.section("Available commands");
        for (Command command : registry.list()) {
            ConsoleUi.item(command.getName() + " (" + command.getUsage() + ")");
        }
    }
}
