# Duelistic Cloud

Duelistic Cloud is a lightweight local control plane for template-based game
servers. It ships with a CLI + HTTP API for managing templates and temporary
servers, plus a Spigot plugin that reports live player counts back to the cloud.

## Components
- `duelistic-cloud`: core service with CLI commands, HTTP API, and auto-renew
  logic for template servers.
- `spigot-plugin`: Bukkit/Spigot plugin that posts player counts to the cloud.

## Requirements
- Java 17+ for `duelistic-cloud`
- Java 21+ for `spigot-plugin`
- Maven 3.8+

## Build
```bash
mvn -f duelistic-cloud/pom.xml clean package
mvn -f spigot-plugin/pom.xml clean package
```

## Run the cloud core
```bash
java -jar duelistic-cloud/target/duelistic-cloud-1.0-SNAPSHOT.jar
```

## CLI commands
- `help` - list available commands
- `setup` - create a `lobby` template from `default/`
- `template add <name>` - create a new template from `default/`
- `template remove <name>` - delete a template
- `template list` - list templates
- `start` - start servers for all templates
- `servers` - list temporary servers and status
- `stop` - stop all servers and shut down the cloud

## HTTP API
The cloud exposes a small local API on `http://127.0.0.1:8080`.

- `GET /health` - readiness check
- `GET /templates` - list template names
- `GET /servers` - list server status + player counts
- `POST /servers` - update player counts
- `POST /start` - start servers for all templates
- `POST /stop` - stop all servers

`POST /servers` expects JSON like:
```json
{"name":"lobby-1","currentPlayers":3,"maxPlayers":50}
```

## Templates and data
- `default/` - seed files copied into new templates
- `system/templates/<name>/template.yml` - template settings
- `system/tmp/<server>/` - generated temporary servers

`template.yml` fields:
- `templateName`
- `maxRamMb`
- `maxPlayers`
- `serverMin`
- `serverMax`

## Spigot plugin setup
1. Build the plugin and drop `spigot-plugin/target/duelistic-cloud-spigot-1.0-SNAPSHOT.jar`
   into your server's `plugins/` folder.
2. Edit `plugins/DuelisticCloud/config.yml` as needed:
   - `cloudUrl`: cloud endpoint for updates (default `http://127.0.0.1:8080/servers`)
   - `updateIntervalTicks`: update frequency in ticks
   - `serverNameOverride`: optional explicit server name
3. Restart the server. The plugin will post player counts asynchronously.

The plugin also reads `template.yml` from the server root (when present) to
align its reported template name and max player count with the cloud settings.

## License
All rights reserved.
