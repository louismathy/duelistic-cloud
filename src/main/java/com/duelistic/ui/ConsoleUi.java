package com.duelistic.ui;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public final class ConsoleUi {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private ConsoleUi() {
    }

    public static void logo() {
        System.out.print("▗▄▄▄ ▗▖ ▗▖▗▄▄▄▖▗▖   ▗▄▄▄▖ ▗▄▄▖▗▄▄▄▖▗▄▄▄▖ ▗▄▄▖\n");
        System.out.print("▐▌  █▐▌ ▐▌▐▌   ▐▌     █  ▐▌     █    █  ▐▌   \n");
        System.out.print("▐▌  █▐▌ ▐▌▐▛▀▀▘▐▌     █   ▝▀▚▖  █    █  ▐▌   \n");
        System.out.print("▐▙▄▄▀▝▚▄▞▘▐▙▄▄▖▐▙▄▄▖▗▄█▄▖▗▄▄▞▘  █  ▗▄█▄▖▝▚▄▄▖\n");
    }

    public static void banner(String title) {
        int width = Math.max(8, title.length() + 8);
        String line = repeat("=", width);
        System.out.println(line);
        System.out.println("== " + title + " ==");
        System.out.println(line);
    }

    public static void info(String message) {
        System.out.println(prefix("INFO") + " " + message);
    }

    public static void success(String message) {
        System.out.println(prefix("OK") + " " + message);
    }

    public static void warn(String message) {
        System.out.println(prefix("WARN") + " " + message);
    }

    public static void error(String message) {
        System.out.println(prefix("ERR") + " " + message);
    }

    public static void section(String title) {
        System.out.println();
        System.out.println(prefix("INFO") + " " + title);
    }

    public static void item(String message) {
        System.out.println("  - " + message);
    }

    public static void prompt(String label) {
        System.out.print(prefix("INPUT") + " " + label + ": ");
    }

    private static String prefix(String level) {
        return "[" + LocalTime.now().format(TIME_FORMAT) + "] [" + level + "]";
    }

    private static String repeat(String token, int count) {
        StringBuilder builder = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            builder.append(token);
        }
        return builder.toString();
    }
}
