package com.duelistic.commands;

import java.io.IOException;
import java.util.List;
import java.util.Scanner;

import com.duelistic.system.CloudDirectories;
import com.duelistic.system.TemplateConfig;
import com.duelistic.ui.ConsoleUi;

/**
 * Manages server templates via CLI subcommands.
 */
public class TemplateCommand implements Command {
    private final CloudDirectories directories;
    private final TemplateConfigPrompter prompter;

    /**
     * Creates the template command with directory helper and input scanner.
     */
    public TemplateCommand(CloudDirectories directories, Scanner scanner) {
        this.directories = directories;
        this.prompter = new TemplateConfigPrompter(scanner);
    }

    /**
     * Returns the CLI command name.
     */
    @Override
    public String getName() {
        return "template";
    }

    /**
     * Dispatches template subcommands (add/remove/list).
     */
    @Override
    public void execute(String[] args) {
        if (args.length == 0) {
            printUsage();
            return;
        }

        String action = args[0].toLowerCase();
        if ("add".equals(action)) {
            handleAdd(args);
            return;
        }
        if ("remove".equals(action)) {
            handleRemove(args);
            return;
        }
        if ("list".equals(action)) {
            handleList();
            return;
        }

        printUsage();
    }

    /**
     * Handles the "template add" flow.
     */
    private void handleAdd(String[] args) {
        if (args.length < 2) {
            ConsoleUi.warn("Missing template name.");
            printUsage();
            return;
        }
        String name = args[1].trim();
        if (name.isEmpty()) {
            ConsoleUi.warn("Template name cannot be empty.");
            return;
        }
        try {
            if (directories.templateExists(name)) {
                ConsoleUi.warn("Template already exists: " + name);
                return;
            }
            TemplateConfig config = prompter.promptConfig(name);
            directories.copyDefaultToTemplate(name);
            config.writeTo(directories.getTemplateConfigFile(name));
            ConsoleUi.success("Template created from default: " + name);
        } catch (IOException e) {
            ConsoleUi.error("Failed to create template: " + e.getMessage());
        }
    }

    /**
     * Handles the "template remove" flow.
     */
    private void handleRemove(String[] args) {
        if (args.length < 2) {
            ConsoleUi.warn("Missing template name.");
            printUsage();
            return;
        }
        String name = args[1].trim();
        if (name.isEmpty()) {
            ConsoleUi.warn("Template name cannot be empty.");
            return;
        }
        try {
            if (!directories.templateExists(name)) {
                ConsoleUi.warn("Template not found: " + name);
                return;
            }
            directories.deleteTemplate(name);
            ConsoleUi.success("Template removed: " + name);
        } catch (IOException e) {
            ConsoleUi.error("Failed to remove template: " + e.getMessage());
        }
    }

    /**
     * Lists available templates.
     */
    private void handleList() {
        try {
            List<String> templates = directories.listTemplates();
            if (templates.isEmpty()) {
                ConsoleUi.warn("No templates found.");
                return;
            }
            ConsoleUi.section("Templates");
            for (String template : templates) {
                ConsoleUi.item(template);
            }
        } catch (IOException e) {
            ConsoleUi.error("Failed to list templates: " + e.getMessage());
        }
    }

    /**
     * Prints CLI usage for template commands.
     */
    private void printUsage() {
        ConsoleUi.section("Usage");
        ConsoleUi.item("template add <name>");
        ConsoleUi.item("template remove <name>");
        ConsoleUi.item("template list");
    }

    public String getUsage() {
        return "template [add / remove] name | template list";
    }
}
