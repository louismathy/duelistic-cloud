package com.duelistic.commands;

import java.io.IOException;
import java.util.Scanner;

import com.duelistic.system.CloudDirectories;
import com.duelistic.system.TemplateConfig;
import com.duelistic.ui.ConsoleUi;

/**
 * Performs initial setup by creating a lobby template.
 */
public class SetupCommand implements Command {
    private final CloudDirectories directories;
    private final TemplateConfigPrompter prompter;

    /**
     * Creates the setup command with directory helper and input scanner.
     */
    public SetupCommand(CloudDirectories directories, Scanner scanner) {
        this.directories = directories;
        this.prompter = new TemplateConfigPrompter(scanner);
    }

    /**
     * Returns the CLI command name.
     */
    @Override
    public String getName() {
        return "setup";
    }

    /**
     * Runs the interactive setup flow.
     */
    @Override
    public void execute(String[] args) {
        try {
            directories.ensureExists();
            if (directories.templateExists("lobby")) {
                ConsoleUi.info("Setup complete. Lobby template already exists.");
                return;
            }
            TemplateConfig config = prompter.promptConfig("lobby");
            directories.copyDefaultToTemplate("lobby");
            config.writeTo(directories.getTemplateConfigFile("lobby"));
            ConsoleUi.success("Setup complete. Created lobby template from default.");
        } catch (IOException e) {
            ConsoleUi.error("Setup failed: " + e.getMessage());
        }
    }
}
