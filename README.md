# MythicRod

## Advanced Fishing Plugin for Minecraft Paper Servers

MythicRod is a comprehensive fishing plugin that replaces vanilla Minecraft fishing with a fully customizable drop system. It features biome-specific drops, permission-based rewards, statistics tracking, and much more.

![MythicRod Banner](https://via.placeholder.com/800x200?text=MythicRod+Advanced+Fishing)

## Features

- **Fully Customizable Drops**: Configure what items players can catch, their chances, and amounts
- **Advanced Item Customization**: Add custom names, lore, enchantments, and visual effects to dropped items
- **Biome-Specific Drops**: Set different items to appear in different biomes for immersive gameplay
- **Permission-Based Drops**: Reward players with special items based on their permissions
- **Statistics Tracking**: Keep track of player fishing stats, rare catches, and more
- **Custom Sound & Visual Effects**: Enhance the fishing experience with custom sounds and particles
- **In-Game Commands**: Manage the plugin directly from in-game
- **Developer API**: Integrate MythicRod with your own plugins

## Commands

| Command                       | Description                     | Permission               |
| ----------------------------- | ------------------------------- | ------------------------ |
| `/mythicrod reload`           | Reload the plugin configuration | `mythicrod.admin.reload` |
| `/mythicrod stats [player]`   | View fishing statistics         | `mythicrod.stats`        |
| `/mythicrod top [limit]`      | View top fishers on the server  | `mythicrod.stats.top`    |
| `/mythicrod drops [category]` | View available fishing drops    | `mythicrod.drops`        |
| `/mythicrod help`             | Display help information        | `mythicrod.command`      |

## Permissions

| Permission                  | Description                  | Default |
| --------------------------- | ---------------------------- | ------- |
| `mythicrod.command`         | Access to basic commands     | `true`  |
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
prefix: "&6&l<MythicRod> &r"
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
    - "DIAMOND,5,1"
    - "IRON_INGOT,30,1"

  # Advanced drops with full customization
  legendary:
    excalibur:
      material: NETHERITE_SWORD
      chance: 1
      amount: 1
      name: "&b&lExcalibur"
      lore:
        - "&7A legendary sword pulled from the depths"
      enchantments:
        sharpness: 10
      glowing: true
      permission: mythicrod.drops.legendary

# Biome-specific drops
biome-drops:
  ocean:
    - "PRISMARINE_CRYSTALS,20,1"
    - "HEART_OF_THE_SEA,5,1"
```

### messages.yml

Contains all plugin messages:

```yaml
fishing:
  rare-catch: "&b&lRARE CATCH! &fYou caught: &e{item}&f!"
  legendary-catch: "&d&lLEGENDARY CATCH! &fYou caught: &e{item}&f!"
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

- Minecraft 1.21.4 or newer
- Paper server

## Changelog

### Version 2.0.0

- Complete rewrite with modular architecture
- Added biome-specific drops
- Added permission-based drops
- Added statistics tracking
- Added advanced item customization
- Added in-game commands
- Added developer API
- Improved performance and stability

### Version 1.0.1

- Initial release

## Support

For support, please open an issue on our GitHub repository or join our Discord server.

## License

MythicRod is licensed under the MIT License - see the LICENSE file for details.
