package com.duelistic.commands;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class CommandRegistry {
    private final Map<String, Command> commands = new LinkedHashMap<>();

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

    public Command find(String name) {
        if (name == null) {
            return null;
        }
        return commands.get(normalize(name));
    }

    public Collection<Command> list() {
        return Collections.unmodifiableCollection(commands.values());
    }

    private String normalize(String name) {
        return name.trim().toLowerCase();
    }
}
