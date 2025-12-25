package com.duelistic;


import java.util.Scanner;

import com.duelistic.commands.CommandRegistry;
import com.duelistic.commands.CommandSystem;
import com.duelistic.commands.HelpCommand;

public class Cloud
{
    private static Cloud instance;
    private Scanner keyScanner;
    private CommandRegistry commandRegistry;
    private CommandSystem commandSystem;
    public static void main(String[] args)
    {
        // Starting
        System.out.println("Duelistic Cloud is starting!");

        instance = new Cloud();
        instance.keyScanner = new Scanner(System.in);
        instance.commandRegistry = new CommandRegistry();
        instance.commandSystem = new CommandSystem(instance.keyScanner, instance.commandRegistry);
        instance.commandRegistry.register(new HelpCommand(instance.commandRegistry));

        // Started
        System.out.println("Duelistic Cloud has started!");
        instance.commandSystem.start();
    }

    public static Cloud getInstance() {
        return instance;
    }

    public Scanner getKeyScanner() {
        return keyScanner;
    }

    public CommandRegistry getCommandRegistry() {
        return commandRegistry;
    }

    public CommandSystem getCommandSystem() {
        return commandSystem;
    }
}
