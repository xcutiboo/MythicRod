# MythicRod

A Paper fishing plugin built around weighted loot tables, biome-aware drops,
permission-gated rarities, persistent stats, an in-game drop editor, and a
small public API for other plugins to integrate with.

[![Paper](https://img.shields.io/badge/Paper-26.1.2-blue)](https://papermc.io)
[![Java](https://img.shields.io/badge/Java-25-orange)](https://adoptium.net)
[![Release](https://img.shields.io/github/v/release/xcutiboo/MythicRod)](https://github.com/xcutiboo/MythicRod/releases)
[![bStats](https://img.shields.io/bstats/servers/23847)](https://bstats.org/plugin/bukkit/MythicRod/23847)
[![License](https://img.shields.io/badge/License-MIT-green)](LICENSE)
[![Docs](https://img.shields.io/badge/docs-mythicrod-blue)](https://xcutiboo.github.io/MythicRod/)
[![Ko-fi](https://img.shields.io/badge/Ko--fi-support-FF5E5B?logo=ko-fi&logoColor=white)](https://ko-fi.com/xcutiboo)

- **Documentation site:** <https://xcutiboo.github.io/MythicRod/>
- **Developer API guide:** [docs/developer-api.md](docs/developer-api.md)
- **Changelog:** [CHANGELOG.md](CHANGELOG.md)
- **Support development:** <https://ko-fi.com/xcutiboo>

## What it does

For **players**, MythicRod adds rarity-tiered catches, fishing rods with
progression, and proper reward feedback (sound, particles, MiniMessage chat
templates) without overhauling vanilla mechanics.

For **server admins**, every drop is editable in-game through a GUI, mirrored
to commands, validated on reload, and protected by namespaced permission
gates. There's a `/mythicrod validate` config-health command, a
`/mythicrod testroll` simulator for tuning weights, and a Folia-safe
`/mythicrod give` to hand out tier rods.

For **plugin developers**, the public API in `mythicrod-api` exposes a
service entry point, immutable drop and stat snapshots, an external drop
provider hook, and Paper events for the reward roll, fish catch, and stats
update lifecycle.

## At a glance

| Topic                | Details                                                                  |
| -------------------- | ------------------------------------------------------------------------ |
| Plugin version       | `26.5.0`                                                                  |
| Target platform      | Paper `26.1.2` (`api-version: 26.1.2`)                                   |
| Java runtime         | Java `25+`                                                               |
| Optional integration | [Nexo](https://polymart.org/resource/nexo) for `nexo:*` item identifiers |
| Bundled languages    | `en_US`, `ja_JP`                                                         |
| Validation baseline  | Official Paper `26.1.2` build `64` on Temurin `25.0.3`                   |
| Scheduler model      | Paper-first with Folia-aware owner scheduling                            |

MythicRod is built for current Paper first. Folia-aware scheduler handoffs are
in place, but I have not yet smoke-tested against a live Folia build. Validate
on your own target server before relying on Folia in production.

Spigot is not officially supported. The module split (`mythicrod-api`,
`mythicrod-common`, `mythicrod-paper`) is structured so a `mythicrod-spigot`
module could be added later without touching the Paper code. I'm holding off
on that work until there's enough support to justify the maintenance burden.
If you want it, consider sponsoring through [Ko-fi](https://ko-fi.com/xcutiboo).

## Highlights

- Weighted reward tables with custom names, lore, enchantments, glow, model data, biome filters, and permission gates.
- GUI and command workflows for drops, config toggles, particles, rod selection, stats, and leaderboard views.
- MiniMessage-based text everywhere, with lenient parsing so bad admin-entered formatting falls back to visible plain text instead of breaking menus.
- Config-backed rod luck multipliers that affect rare and legendary rolls without distorting common catches.
- Persisted player statistics by rarity tier, with API snapshots and leaderboard access.
- Particle validation against the running Paper API, including safe defaults for data-bound particles.
- API hooks for external reward providers plus `MythicRodFishCatchEvent` and `MythicRodRewardRollEvent`.

## Quick Start

1. Download `MythicRod-Paper-x.y.z.jar` from [Releases](https://github.com/xcutiboo/MythicRod/releases).
2. Drop it into your server's `plugins/` directory.
3. Start the server once.
4. Edit `plugins/MythicRod/config.yml` and `plugins/MythicRod/drops.yml`.
5. Run `/mythicrod reload` after config changes.

No extra runtime dependencies are required.

## Admin Workflow

- `/mythicrod` or `/mythicrod gui` opens the main hub.
- `/mythicrod drops` opens the drop browser and editor.
- `/mythicrod config` mirrors the core runtime toggles available in the GUI.
- `/mythicrod rod` opens rod-tier and visual-preference controls.
- `/mythicrod stats` and `/mythicrod top` expose persisted player stats.

The drop editor is built for real admin work, not only toy examples. It can edit
identifier, weight, amount, display name, lore, model data, enchantments, item
flags, permission gate, biome filter, and glow state, then save back to
`plugins/MythicRod/drops.yml`.

Text-entry fields use chat input, so admins can enter exact values instead of
clicking through tiny increments:

- Item identifiers: `DIAMOND`, `minecraft:diamond`, or `nexo:item_id`
- Exact numbers: weight and amount
- MiniMessage display names and lore
- Permission nodes or `clear`
- Biome lists such as `ocean, deep_ocean`
- Enchantments such as `sharpness:3, unbreaking:2`
- Item flags such as `HIDE_ENCHANTS, HIDE_ATTRIBUTES`

`cancel`, `back`, and `exit` return to the editor without applying the typed
value.

## Commands

| Command                                | Description                                       | Permission                                            |
| -------------------------------------- | ------------------------------------------------- | ----------------------------------------------------- |
| `/mythicrod`                           | Open the main GUI                                 | `mythicrod.command`                                   |
| `/mythicrod gui`                       | Open the main GUI directly                        | `mythicrod.gui`                                       |
| `/mythicrod rod`                       | Open rod and visual settings                      | `mythicrod.gui`                                       |
| `/mythicrod reload`                    | Reload config, drops, players, and language files | `mythicrod.admin.reload`                              |
| `/mythicrod stats [player]`            | View fishing statistics                           | `mythicrod.stats.view`, `mythicrod.stats.view.others` |
| `/mythicrod top [limit]`               | View the leaderboard                              | `mythicrod.stats.leaderboard`                         |
| `/mythicrod drops [category]`          | Browse drop tables or open a category directly    | `mythicrod.drops.view`                                |
| `/mythicrod give <player> <tier>`      | Give a MythicRod item                             | `mythicrod.admin.give`                                |
| `/mythicrod config [setting] [value]`  | View or edit core settings                        | `mythicrod.admin.config`                              |
| `/mythicrod particle [channel] <type>` | Configure fishing particles                       | `mythicrod.admin.config`                              |
| `/mythicrod validate`                  | Run a health check on loaded drops                | `mythicrod.admin.config`                              |
| `/mythicrod testroll [biome] [count]`  | Simulate loot rolls and print a tier histogram    | `mythicrod.admin.debug`                               |
| `/mythicrod rod inspect`               | Dump MythicRod metadata for the held rod          | `mythicrod.admin.debug`                               |
| `/mythicrod debug`                     | Print debug info to console                       | `mythicrod.admin.debug`                               |
| `/mythicrod help`                      | Show the command reference                        | `mythicrod.command`                                   |

Config commands cover the same core toggles exposed by the GUI:
`sounds`, `particles`, `statistics`, `biome-drops`, `permissions`, `debug`,
`delivery-mode`, and `stats-save-interval`.

Particle commands are validated against the running Paper API. If a selected
particle requires extra data, MythicRod supplies safe defaults so a bad choice
does not break reward delivery.

## Permissions

| Permission                    | Default | Purpose                     |
| ----------------------------- | ------- | --------------------------- |
| `mythicrod.command`           | `true`  | Base command access         |
| `mythicrod.gui`               | `true`  | Open the main GUI           |
| `mythicrod.stats.view`        | `true`  | View your own stats         |
| `mythicrod.stats.view.others` | `op`    | View another player's stats |
| `mythicrod.stats.leaderboard` | `true`  | View the leaderboard        |
| `mythicrod.drops.view`        | `true`  | Browse drop tables          |
| `mythicrod.admin.reload`      | `op`    | Reload runtime data         |
| `mythicrod.admin.give`        | `op`    | Give MythicRod items        |
| `mythicrod.admin.config`      | `op`    | Edit drops and config       |
| `mythicrod.admin.debug`       | `op`    | Print debug information     |
| `mythicrod.rod.advanced`      | `op`    | Use the Advanced rod tier   |
| `mythicrod.rod.legendary`     | `op`    | Use the Legendary rod tier  |

MythicRod also exposes grouped trees such as `mythicrod.*`, `mythicrod.admin.*`,
`mythicrod.stats.*`, `mythicrod.drops.*`, and `mythicrod.rod.*` in
`paper-plugin.yml`.

## Configuration

### `config.yml`

```yaml
language:
  default: en_US # en_US | ja_JP

features:
  sounds:
    enabled: true

  particles:
    enabled: true
    catch-particle: SPLASH
    bubble-particle: BUBBLE_POP
    success-particle: HAPPY_VILLAGER
    xp-particle: HAPPY_VILLAGER

  statistics:
    enabled: true

  drops:
    biome-specific:
      enabled: true
    delivery-mode: vanilla_retrieve # vanilla_retrieve | inventory | drop_at_player

  rods:
    luck-multipliers:
      basic: 1.0
      advanced: 1.25
      legendary: 1.5

  permissions:
    enabled: true

  debug:
    enabled: false

timers:
  stats-save-interval-seconds: 600

messages:
  catch:
    common: '<gray>You caught <white><bold>{amount}x {item}</bold></white>!'
    uncommon: |-
      <green><bold>♦ Uncommon Catch ♦</bold></green>
      <dark_green>You caught <green><bold>{amount}x {item}</bold></green>!
    rare: |-
      <aqua><bold>★ Rare Catch! ★</bold></aqua>
      <dark_aqua>You caught <aqua><bold>{amount}x {item}</bold></aqua>!
    legendary: |-
      <gold><bold>✨ LEGENDARY CATCH! ✨</bold></gold>
      <yellow>You caught <gold><bold>{amount}x {item}</bold></gold>!
```

Notes:

- `weight` is a relative roll weight, not a percentage.
- Rod luck multipliers affect only rare and legendary reward weights.
- Invalid particle names are corrected to safe defaults at startup or reload.
- Delivery mode is configured under `features.drops.delivery-mode`.

### `drops.yml`

```yaml
drops:
  global:
    - identifier: COD
      weight: 50
      amount: 1

    - identifier: SALMON
      weight: 30
      amount: 1
      custom_name: '<aqua>★ Silver Salmon</aqua>'
      lore:
        - '<gray>A shimmering silver catch'

  rare:
    - identifier: DIAMOND
      weight: 2
      amount: 1
      custom_name: '<aqua>Deep-Sea Diamond</aqua>'
      glow: true

  biome_ocean:
    - identifier: NAUTILUS_SHELL
      weight: 5
      amount: 1
      biomes:
        - minecraft:ocean
        - minecraft:deep_ocean
```

| Field               | Type                   | Meaning                                                        |
| ------------------- | ---------------------- | -------------------------------------------------------------- |
| `identifier`        | `String`               | Material name, `minecraft:*`, or `nexo:*` when Nexo is enabled |
| `weight`            | `int`                  | Relative roll weight                                           |
| `amount`            | `int`                  | Stack size                                                     |
| `custom_name`       | `String`               | MiniMessage display name                                       |
| `lore`              | `List<String>`         | MiniMessage lore lines                                         |
| `custom_model_data` | `int`                  | Custom model data, `0` or omitted to disable                   |
| `glow`              | `boolean`              | Enchantment glow without a visible enchantment                 |
| `enchantments`      | `Map<String, Integer>` | Example: `'minecraft:unbreaking': 2`                           |
| `item_flags`        | `List<String>`         | Bukkit item flags such as `HIDE_ENCHANTS`                      |
| `biomes`            | `List<String>`         | Restrict a drop to specific biomes                             |
| `permission`        | `String`               | Permission node required to catch the drop                     |

Category notes:

- Biome categories use a `biome_` prefix, such as `biome_ocean`.
- `/mythicrod drops ocean` resolves to the same category as `/mythicrod drops biome_ocean`.
- With `features.permissions.enabled: true`, MythicRod applies default gates for `global`, `rare`, and `legendary` unless a row defines its own `permission`.
- Older `biome-drops.*` configs are still read during upgrade, but modern `drops.*` categories win when both exist.

## Developer API

MythicRod exposes a small but practical integration surface built around one
runtime service, two Paper events, and platform-neutral item/drop/stat views.

| If you want to...                 | Use                                                                |
| --------------------------------- | ------------------------------------------------------------------ |
| Resolve MythicRod at runtime      | `MythicRodAPI` via Bukkit `ServicesManager` or `MythicRodServices` |
| Inspect loaded drop tables        | `DropCatalog` and `PlatformDrop`                                   |
| Create MythicRod-compatible items | `MythicRodAPI#createItem(...)` or `PlatformItemFactory`            |
| Inject custom rewards             | `ExternalDropProvider`                                             |
| Read stats and leaderboards       | `PlayerStatSnapshot` futures from `MythicRodAPI`                   |
| Intercept Paper reward flow       | `MythicRodRewardRollEvent` and `MythicRodFishCatchEvent`           |

Preferred Paper lookup:

```java
MythicRodAPI api = MythicRodServices.require();
```

Simple provider registration:

```java
api.registerExternalDropProvider(new ExternalDropProvider() {
  @Override
  public String getKey() {
    return "myplugin:special_drops";
  }

  @Override
  public double getWeight(PlatformPlayer player) {
    return player.hasPermission("myplugin.special_reward") ? 2.5D : 0.0D;
  }

  @Override
  public PlatformItem generateItem(PlatformPlayer player) {
    return api.createItem("DIAMOND", 1).orElse(null);
  }

  @Override
  public String getDisplayName() {
    return "<aqua>Special Diamond</aqua>";
  }

  @Override
  public String getTier() {
    return "rare";
  }
});
```

Important contract notes:

- Future-backed API methods complete on MythicRod's async scheduler, not on a safe world or entity owner thread.
- Provider hooks and Paper events run on MythicRod's player-owned execution path and must stay non-blocking.
- Reward weights are relative weights, not percentages.

Full guide: [docs/developer-api.md](docs/developer-api.md)

## Localization

Translations are managed on Crowdin:
[crowdin.com/project/mythicrod](https://crowdin.com/project/mythicrod). Source
language is `en_US`; everything else syncs back into the repo through an
automated `l10n: sync Crowdin translations` pull request.

To use a translation on your server:

1. Copy `plugins/MythicRod/lang/en_US.yml` to a new locale file such as `de_DE.yml`.
2. Translate values only. Do not rename keys.
3. Set `language.default` in `config.yml`.
4. Run `/mythicrod reload`.

Repository language sources live in `mythicrod-paper/src/main/resources/lang/`.

All player-facing text uses [MiniMessage](https://docs.advntr.dev/minimessage/format.html):

```text
'<gold><bold>My text</bold></gold>'
'<#ff6600>Hex colors too!</#ff6600>'
```

MythicRod deliberately ignores stale unknown keys from copied older language
files so dead text cannot override current GUI and command behavior.

## Metrics And Publishing

MythicRod uses bStats (`pluginId: 23847`) for anonymous adoption and health
signals such as Paper version, language, delivery mode, feature toggles,
configured drop count, tracked player count, and total custom catches. bStats
startup is isolated so a metrics issue cannot stop the plugin from enabling.

Hangar publishing is wired through the official Paper Hangar Gradle plugin.
Store the token in `HANGAR_API_TOKEN`. Do not place it in source files,
workflow YAML, or `gradle.properties`.

```bash
./gradlew :mythicrod-paper:build \
  :mythicrod-paper:publishPluginPublicationToHangar \
  :mythicrod-paper:syncPluginPublicationMainResourcePagePageToHangar
```

The release workflow uses the shaded Paper jar, `README.md` as the resource
page, `CHANGELOG.md` as the release changelog, and the `paperVersion` declared
in `gradle.properties`.

## Build From Source

```bash
git clone https://github.com/xcutiboo/MythicRod.git
cd MythicRod
export JAVA_HOME="$HOME/.local/java/jdk-25"
./gradlew clean :mythicrod-paper:shadowJar
```

Output:
`mythicrod-paper/build/libs/MythicRod-Paper-x.y.z.jar`

Project layout:

```text
MythicRod/
|- mythicrod-api/      public API, service contracts, platform-facing DTOs
|- mythicrod-common/   shared drop logic, config, stats, and runtime policy
`- mythicrod-paper/    Paper implementation, GUI, commands, events, adapters
```

## Support

If MythicRod is useful to you and you want me to keep building it (in
particular the Spigot module and more in-depth Folia validation), the easiest
way to help is [Ko-fi](https://ko-fi.com/xcutiboo). Bug reports and PRs are
just as welcome and free.

## License

MIT. See [LICENSE](LICENSE).
