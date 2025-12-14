# MythicRod

A fishing plugin for Minecraft Paper servers with custom drops, biome-specific loot, and player statistics.

## Features

- Custom fishing drops with configurable chances and amounts
- Biome-specific loot tables
- Permission-based drops for VIP players
- Statistics tracking and leaderboards
- Item customization (names, lore, enchantments, glow effects)
- Sound and particle effects
- In-game GUI for managing settings and viewing stats
- Multi-language support (English and Japanese included)
- Developer API for integrations
- Built for Paper 1.21.4+ with no deprecated API usage

## Commands

| Command                          | Description                     | Permission               |
| -------------------------------- | ------------------------------- | ------------------------ |
| `/mythicrod` or `/mythicrod gui` | Open the main GUI menu          | `mythicrod.gui`          |
| `/mythicrod reload`              | Reload the plugin configuration | `mythicrod.admin.reload` |
| `/mythicrod stats [player]`      | View fishing statistics         | `mythicrod.stats`        |
| `/mythicrod top [limit]`         | View top fishers on the server  | `mythicrod.stats.top`    |
| `/mythicrod drops [category]`    | View available fishing drops    | `mythicrod.drops`        |
| `/mythicrod help`                | Display help information        | `mythicrod.command`      |

## Permissions

| Permission                  | Description                  | Default |
| --------------------------- | ---------------------------- | ------- |
| `mythicrod.command`         | Access to basic commands     | `true`  |
| `mythicrod.gui`             | Access to GUI menus          | `true`  |
| `mythicrod.admin.reload`    | Ability to reload the plugin | `op`    |
| `mythicrod.stats`           | View fishing statistics      | `true`  |
| `mythicrod.stats.top`       | View top fishers             | `true`  |
| `mythicrod.drops`           | View available drops         | `true`  |
| `mythicrod.drops.global`    | Access to global drops       | `true`  |
| `mythicrod.drops.rare`      | Access to rare drops         | `op`    |
| `mythicrod.drops.legendary` | Access to legendary drops    | `op`    |

## Configuration

MythicRod uses three main configuration files:

### config.yml

Contains basic plugin settings:

```yaml
# Visual settings
prefix: '&6&l<MythicRod> &r'
use-sounds: true
use-particles: true

# Core features
track-statistics: true
enable-biome-specific-drops: true
use-permissions: false
```

### drops.yml

Defines all custom drops:

```yaml
# Global drops are available to all players
drops:
  global:
    - 'DIAMOND,5,1'
    - 'IRON_INGOT,30,1'

  # Advanced drops with full customization
  legendary:
    excalibur:
      material: NETHERITE_SWORD
      chance: 1
      amount: 1
      name: '&b&lExcalibur'
      lore:
        - '&7A legendary sword pulled from the depths'
      enchantments:
        sharpness: 10
      glowing: true
      permission: mythicrod.drops.legendary

# Biome-specific drops
biome-drops:
  ocean:
    - 'PRISMARINE_CRYSTALS,20,1'
    - 'HEART_OF_THE_SEA,5,1'
```

### messages.yml

Contains all plugin messages:

```yaml
fishing:
  rare-catch: '&b&lRARE CATCH! &fYou caught: &e{item}&f!'
  legendary-catch: '&d&lLEGENDARY CATCH! &fYou caught: &e{item}&f!'
```

## For Developers

MythicRod provides a full API for plugin integration:

```java
// Get the API instance
MythicRodAPI api = plugin.getAPI();

// Get a random drop for a player
ItemStack item = api.getRandomDrop(player, "OCEAN");

// Record a catch for statistics
api.recordCatch(player, Material.DIAMOND, 1);

// Get player statistics
Map<String, Object> stats = api.getPlayerStatistics(player);
```

## Installation

1. Download the plugin JAR file
2. Place it in your server's `plugins` folder
3. Restart the server
4. Edit the configuration files to customize the plugin
5. Use `/mythicrod reload` to apply changes

## Requirements

- Paper 1.21.4+ or Spigot/Purpur 1.21.1+
- Java 21+

## Building

```bash
./gradlew clean build
```

The compiled JAR will be in `build/libs/`.

## Support

Open an issue on GitHub or join the Discord server.

## License

MIT License - see LICENSE file for details.
