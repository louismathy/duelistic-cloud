# Duelistic Cloud

Duelistic Cloud is a lightweight cloud/server management system for Hytale.
It provides a simple command-driven core for starting, monitoring, and managing
server-side services.

## Features
- Command system with pluggable commands
- Interactive CLI via standard input
- Simple registry for registering custom commands

## Requirements
- Java 8+ (compatible with Maven)

## Build
```bash
mvn clean package
```

## Run
```bash
mvn exec:java -Dexec.mainClass="com.duelistic.Cloud"
```

## Commands
Type commands into the console. The built-in `help` command prints the list of
registered commands.

## Extending
Create a class that implements `com.duelistic.commands.Command`, then register
it via `CommandRegistry.register(...)` on startup.

## Project Layout
- `src/main/java/com/duelistic/Cloud.java` entry point
- `src/main/java/com/duelistic/commands` command system

## License
All rights reserved. Update this section as needed.
