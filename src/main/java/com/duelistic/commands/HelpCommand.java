package com.duelistic.commands;

public class HelpCommand implements Command {
    private final CommandRegistry registry;

    public HelpCommand(CommandRegistry registry) {
        this.registry = registry;
    }

    @Override
    public String getName() {
        return "help";
    }

    @Override
    public void execute(String[] args) {
        com.duelistic.ui.ConsoleUi.section("Available commands");
        for (Command command : registry.list()) {
            com.duelistic.ui.ConsoleUi.item(command.getName());
        }
    }
}
