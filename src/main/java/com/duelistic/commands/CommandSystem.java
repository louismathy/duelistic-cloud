package com.duelistic.commands;

import java.util.Arrays;
import java.util.Scanner;

import com.duelistic.ui.ConsoleUi;

public class CommandSystem {
    private final Scanner scanner;
    private final CommandRegistry registry;
    private boolean running;

    public CommandSystem(Scanner scanner, CommandRegistry registry) {
        this.scanner = scanner;
        this.registry = registry;
    }

    public void start() {
        running = true;
        ConsoleUi.info("CLI ready. Enter a command.");
        while (running && scanner.hasNextLine()) {
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) {
                continue;
            }
            String[] parts = line.split("\\s+");
            String name = parts[0];
            String[] args = Arrays.copyOfRange(parts, 1, parts.length);
            Command command = registry.find(name);
            if (command == null) {
                ConsoleUi.warn("Unknown command: " + name + " (try 'help')");
                continue;
            }
            command.execute(args);
        }
    }

    public void stop() {
        running = false;
    }
}
