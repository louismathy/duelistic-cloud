package com.duelistic.commands;

import java.util.Scanner;

import com.duelistic.system.TemplateConfig;
import com.duelistic.ui.ConsoleUi;

/**
 * Prompts the operator for template configuration values.
 */
public class TemplateConfigPrompter {
    private final Scanner scanner;

    /**
     * Creates a prompter that reads from the provided scanner.
     */
    public TemplateConfigPrompter(Scanner scanner) {
        this.scanner = scanner;
    }

    /**
     * Prompts for all required template values.
     */
    public TemplateConfig promptConfig(String templateName) {
        int maxRamMb = promptInt("Max RAM in MB");
        int maxPlayers = promptInt("Max players");
        int serverMin = promptInt("Server min");
        int serverMax = promptInt("Server max");
        return new TemplateConfig(templateName, maxRamMb, maxPlayers, serverMin, serverMax);
    }

    /**
     * Prompts until a valid non-negative integer is entered.
     */
    private int promptInt(String label) {
        while (true) {
            ConsoleUi.prompt(label);
            String input = scanner.nextLine().trim();
            try {
                int value = Integer.parseInt(input);
                if (value < 0) {
                    ConsoleUi.warn("Please enter a non-negative number.");
                    continue;
                }
                return value;
            } catch (NumberFormatException e) {
                ConsoleUi.warn("Please enter a valid number.");
            }
        }
    }
}
