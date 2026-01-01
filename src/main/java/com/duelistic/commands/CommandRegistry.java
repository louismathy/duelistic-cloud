package com.duelistic.commands;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Stores available commands by normalized name.
 */
public class CommandRegistry {
    private final Map<String, Command> commands = new LinkedHashMap<>();

    /**
     * Registers a command for later lookup.
     */
    public void register(Command command) {
        if (command == null) {
            throw new IllegalArgumentException("command");
        }
        String name = normalize(command.getName());
        if (name.isEmpty()) {
            throw new IllegalArgumentException("command name");
        }
        commands.put(name, command);
    }

    /**
     * Finds a command by name, or null if unknown.
     */
    public Command find(String name) {
        if (name == null) {
            return null;
        }
        return commands.get(normalize(name));
    }

    /**
     * Returns an immutable view of registered commands.
     */
    public Collection<Command> list() {
        return Collections.unmodifiableCollection(commands.values());
    }

    /**
     * Normalizes command names for case-insensitive lookup.
     */
    private String normalize(String name) {
        return name.trim().toLowerCase();
    }
}
