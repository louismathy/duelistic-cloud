package com.duelistic.commands;

import java.util.Arrays;
import java.util.Scanner;

public class CommandSystem {
    private final Scanner scanner;
    private final CommandRegistry registry;

    public CommandSystem(Scanner scanner, CommandRegistry registry) {
        this.scanner = scanner;
        this.registry = registry;
    }

    public void start() {
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) {
                continue;
            }
            String[] parts = line.split("\\s+");
            String name = parts[0];
            String[] args = Arrays.copyOfRange(parts, 1, parts.length);
            Command command = registry.find(name);
            if (command == null) {
                System.out.println("Unknown command: " + name);
                continue;
            }
            command.execute(args);
        }
    }
}
