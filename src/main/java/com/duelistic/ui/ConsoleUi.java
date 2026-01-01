package com.duelistic.ui;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Utility for consistent console output with timestamps and formatting.
 */
public final class ConsoleUi {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    /**
     * Prints the ASCII logo to the console.
     */
    public static void logo() {
        System.out.print("▗▄▄▄ ▗▖ ▗▖▗▄▄▄▖▗▖   ▗▄▄▄▖ ▗▄▄▖▗▄▄▄▖▗▄▄▄▖ ▗▄▄▖\n");
        System.out.print("▐▌  █▐▌ ▐▌▐▌   ▐▌     █  ▐▌     █    █  ▐▌   \n");
        System.out.print("▐▌  █▐▌ ▐▌▐▛▀▀▘▐▌     █   ▝▀▚▖  █    █  ▐▌   \n");
        System.out.print("▐▙▄▄▀▝▚▄▞▘▐▙▄▄▖▐▙▄▄▖▗▄█▄▖▗▄▄▞▘  █  ▗▄█▄▖▝▚▄▄▖\n");
    }

    /**
     * Prints a banner around the provided title.
     */
    public static void banner(String title) {
        int width = Math.max(8, title.length() + 8);
        String line = repeat("=", width);
        System.out.println(line);
        System.out.println("== " + title + " ==");
        System.out.println(line);
    }

    /**
     * Prints an info-level message.
     */
    public static void info(String message) {
        System.out.println(prefix("INFO") + " " + message);
    }

    /**
     * Prints a success-level message.
     */
    public static void success(String message) {
        System.out.println(prefix("OK") + " " + message);
    }

    /**
     * Prints a warning-level message.
     */
    public static void warn(String message) {
        System.out.println(prefix("WARN") + " " + message);
    }

    /**
     * Prints an error-level message.
     */
    public static void error(String message) {
        System.out.println(prefix("ERR") + " " + message);
    }

    /**
     * Prints a section header with spacing.
     */
    public static void section(String title) {
        System.out.println();
        System.out.println(prefix("INFO") + " " + title);
    }

    /**
     * Prints a list item line.
     */
    public static void item(String message) {
        System.out.println("  - " + message);
    }

    /**
     * Prints a prompt without a trailing newline.
     */
    public static void prompt(String label) {
        System.out.print(prefix("INPUT") + " " + label + ": ");
    }

    /**
     * Builds a timestamped log prefix.
     */
    private static String prefix(String level) {
        return "[" + LocalTime.now().format(TIME_FORMAT) + "] [" + level + "]";
    }

    /**
     * Repeats a token a given number of times.
     */
    private static String repeat(String token, int count) {
        StringBuilder builder = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            builder.append(token);
        }
        return builder.toString();
    }
}
