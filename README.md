# MythicRod

> A professional custom fishing plugin for Paper 1.21.11+ — biome-aware drops, in-game GUI editor, full statistics tracking, and a clean developer API.

[![Paper](https://img.shields.io/badge/Paper-1.21.11+-blue)](https://papermc.io)
[![Java](https://img.shields.io/badge/Java-21-orange)](https://adoptium.net)
[![License](https://img.shields.io/badge/License-MIT-green)](LICENSE)
[![bStats](https://img.shields.io/bstats/servers/23847)](https://bstats.org/plugin/bukkit/MythicRod/23847)

---

## Features

- **Weighted Drop Tables** — configure per-category drops with chance weights, custom names, lore, enchantments, and glow effects
- **Biome-Aware Loot** — different drop pools per biome (`biome_ocean`, `biome_jungle`, etc.)
- **In-Game GUI Editor** — admins can tweak drop chance, amount, name, and lore without touching a YAML file
- **Statistics & Leaderboard** — per-player catch counters by tier (common / uncommon / rare / legendary), persisted to disk
- **Rarity Effects** — tiered particles and sounds on every catch; legendary catches play a full celebration sequence
- **MiniMessage Throughout** — all text uses Adventure's MiniMessage; no legacy `§` codes anywhere
- **Multi-Language** — ships with `en_US` and `ja_JP`; add your own locale by dropping a YAML into `lang/`
- **Brigadier Commands** — native Paper Brigadier command registration with tab-completion
- **Folia-Compatible Scheduling** — all entity/player callbacks use the correct regional scheduler
- **Developer API** — register external drop providers, query player stats, hook into `MythicRodFishCatchEvent` and `MythicRodRewardRollEvent`

---

## Requirements

| Dependency | Version |
|---|---|
| Paper | 1.21.11+ |
| Java | 21+ |

No other dependencies required. Optional soft-dependency on [Nexo](https://polymart.org/resource/nexo) for custom item support.

---

## Installation

1. Download `MythicRod-Paper-x.y.z.jar` from [Releases](https://github.com/xcutiboo/MythicRod/releases)
2. Drop it into your server's `plugins/` directory
3. Restart (or `/reload confirm` if you know what you're doing)
4. Edit `plugins/MythicRod/config.yml` and `drops.yml` to your liking
5. `/mythicrod reload` — no restart needed after config changes

---

## Commands

| Command | Description | Permission |
|---|---|---|
| `/mythicrod` | Open main GUI | `mythicrod.command` |
| `/mythicrod gui` | Open main GUI | `mythicrod.gui` |
| `/mythicrod reload` | Reload all configuration | `mythicrod.admin.reload` |
| `/mythicrod stats [player]` | View fishing statistics | `mythicrod.stats.view` |
| `/mythicrod top [limit]` | View leaderboard | `mythicrod.stats.leaderboard` |
| `/mythicrod drops [category]` | Browse drop tables | `mythicrod.drops.view` |
| `/mythicrod give <player> <tier>` | Give a MythicRod item | `mythicrod.admin.give` |
| `/mythicrod debug` | Print debug info to console | `mythicrod.admin.debug` |
| `/mythicrod help` | Show command reference | `mythicrod.command` |

### Permissions

```yaml
mythicrod.command           # Base command access (default: true)
mythicrod.gui               # Open the main GUI (default: true)
mythicrod.stats.view        # View own/others' stats (default: true)
mythicrod.stats.leaderboard # View leaderboard (default: true)
mythicrod.drops.view        # Browse drop tables (default: true)
mythicrod.admin             # All admin permissions (default: op)
mythicrod.admin.reload      # Reload configuration
mythicrod.admin.give        # Give MythicRod items to players
mythicrod.admin.config      # Edit drops via GUI / access config menu
mythicrod.admin.debug       # View debug information
```

---

## Configuration

### `config.yml` — core settings

```yaml
language: en_US          # en_US | ja_JP
use_sounds: true
use_particles: true
track_statistics: true
biome_specific_drops: true
drop_to_inventory: false  # false = fly-to-player (vanilla feel)
debug: false

# Catch message templates (MiniMessage)
messages:
  catch:
    common:    '<green>✓ <gray>Caught <white>%amount%× %item%<gray>!'
    uncommon:  '<aqua>✦ <bold>RARE CATCH!</bold> <white>...'
    rare:      '<aqua>✦ <bold>RARE CATCH!</bold> <white>...'
    legendary: '<light_purple>✦ <bold>LEGENDARY!</bold> <white>...'
```

### `drops.yml` — loot tables

```yaml
drops:
  global:
    - identifier: COD
      chance: 50
      amount: 1

    - identifier: SALMON
      chance: 30
      amount: 1
      custom_name: '<aqua>★ Silver Salmon</aqua>'
      lore:
        - '<gray>A shimmering silver catch'
      glow: false

  rare:
    - identifier: DIAMOND
      chance: 2
      amount: 1
      custom_name: '<aqua>💎 Deep-Sea Diamond</aqua>'
      glow: true

  biome_ocean:
    - identifier: NAUTILUS_SHELL
      chance: 5
      amount: 1
      biomes:
        - minecraft:ocean
        - minecraft:deep_ocean
```

**Drop fields:**

| Field | Type | Description |
|---|---|---|
| `identifier` | String | Material name (e.g. `DIAMOND`) or namespaced key |
| `chance` | int | Relative weight (higher = more common) |
| `amount` | int | Stack size |
| `custom_name` | String | MiniMessage name (optional) |
| `lore` | List\<String\> | MiniMessage lore lines (optional) |
| `glow` | boolean | Enchantment glow without enchantment (optional) |
| `enchantments` | Map | `EFFICIENCY: 3` etc. (optional) |
| `biomes` | List\<String\> | Restrict to specific biomes (optional) |
| `permission` | String | Permission node required to catch (optional) |

---

## Building from Source

```bash
git clone https://github.com/xcutiboo/MythicRod.git
cd MythicRod
./gradlew clean :mythicrod-paper:shadowJar
```

Output: `mythicrod-paper/build/libs/MythicRod-Paper-x.y.z.jar`

### Project Structure

```
MythicRod/
├── mythicrod-common/      # Platform-agnostic core (drop logic, config, stats, API)
└── mythicrod-paper/       # Paper implementation (GUI, Brigadier commands, Folia scheduler)
```

---

## Developer API

Add MythicRod as a dependency (it's registered in Bukkit's `ServicesManager`):

```java
// Retrieve API — works without depending on MythicRod's internal classes
RegisteredServiceProvider<MythicRodAPI> provider =
    Bukkit.getServicesManager().getRegistration(MythicRodAPI.class);

if (provider != null) {
    MythicRodAPI api = provider.getProvider();
}
```

### Register a custom drop provider

```java
api.registerExternalDropProvider(new ExternalDropProvider() {
    @Override
    public String getKey() { return "myplugin:special_drops"; }

    @Override
    public Optional<CustomDrop> getRandomDrop(Player player, String biome) {
        // Your logic here
        return Optional.empty();
    }
});
```

### Query player statistics

```java
// Returns a CompletableFuture<PlayerStatSnapshot>
api.getPlayerStats(player.getUniqueId()).thenAccept(snapshot -> {
    long total     = snapshot.totalCatches();
    long legendary = snapshot.legendaryCatches();
});
```

### Hook into fishing events

```java
// Fired before the weighted roll — modify luck or force a specific drop
@EventHandler
public void onRewardRoll(MythicRodRewardRollEvent event) {
    if (event.getPlayer().hasPermission("vip.luck_boost")) {
        event.setLuckMultiplier(1.5);
    }
}

// Fired after a drop is selected — cancellable, item can be replaced
@EventHandler
public void onFishCatch(MythicRodFishCatchEvent event) {
    event.setRewardItem(myCustomItem);  // replace reward
    // event.setCancelled(true);        // suppress reward entirely
}
```

### Top fishers leaderboard

```java
api.getTopPlayers(StatType.TOTAL_CATCHES, 10).thenAccept(list -> {
    for (PlayerStatSnapshot entry : list) {
        Bukkit.getLogger().info(entry.playerName() + ": " + entry.totalCatches());
    }
});
```

---

## Adding a Language

1. Copy `plugins/MythicRod/lang/en_US.yml` to `lang/de_DE.yml` (or any locale code)
2. Translate every value — **do not change the keys**
3. Set `language: de_DE` in `config.yml`
4. `/mythicrod reload`

All text uses [MiniMessage](https://docs.advntr.dev/minimessage/format.html) formatting. Example:
```
'<gold><bold>My text</bold></gold>'
'<#ff6600>Hex colors too!</#ff6600>'
```

---

## License

[MIT](LICENSE) — free to use, modify, and redistribute. Attribution appreciated.
