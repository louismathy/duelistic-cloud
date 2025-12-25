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
        System.out.println("Available commands:");
        for (Command command : registry.list()) {
            System.out.println("- " + command.getName());
        }
    }
}
