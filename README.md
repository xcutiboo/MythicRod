# MythicRod

Custom fishing plugin with biome-specific drops, statistics tracking, and GUI menus.

## Features

- Custom fishing drops with configurable chances
- Biome-specific loot tables
- Permission-based drop categories
- Statistics tracking and leaderboards
- Item customization (names, lore, enchantments, glow)
- In-game GUI menus
- Multi-language support (English, Japanese)
- Sound and particle effects
- Developer API

## Requirements

- **Paper:** 1.21.4+ (Java 21+)
- **Spigot:** 1.21+ (Java 21+)

## Installation

1. Download the appropriate JAR for your server
2. Place in `plugins/` folder
3. Restart server
4. Configure in `plugins/MythicRod/`

## Commands

| Command                          | Description             | Permission                    |
| -------------------------------- | ----------------------- | ----------------------------- |
| `/mythicrod` or `/mythicrod gui` | Open main menu          | `mythicrod.gui`               |
| `/mythicrod reload`              | Reload configuration    | `mythicrod.admin.reload`      |
| `/mythicrod stats [player]`      | View fishing statistics | `mythicrod.stats.view`        |
| `/mythicrod top [limit]`         | View leaderboard        | `mythicrod.stats.leaderboard` |
| `/mythicrod drops [category]`    | View available drops    | `mythicrod.drops.view`        |
| `/mythicrod help`                | Show help               | `mythicrod.command`           |

## Building

```bash
./gradlew clean build
```

Outputs:

- `mythicrod-paper/build/libs/MythicRod-Paper-x.y.z.jar`
- `mythicrod-spigot/build/libs/MythicRod-Spigot-x.y.z.jar`

## Developer API

```java
MythicRodAPI api = MythicRod.getInstance().getAPI();

// Get random drop for player
ItemStack item = api.getRandomDrop(player, "ocean");

// Record catch
api.recordCatch(player, Material.DIAMOND, 1);

// Get stats
Map<String, Object> stats = api.getPlayerStatistics(player);
```

## License

MIT
