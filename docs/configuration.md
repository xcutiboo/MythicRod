---
title: Configuration
nav_order: 5
---

# Configuration

![divider]({{ site.baseurl }}/assets/divider.svg)

Four runtime files live in `plugins/MythicRod/`:

| File | Holds |
|---|---|
| `config.yml` | Feature toggles, particles, language, rod multipliers, intervals |
| `drops.yml` | Drop categories, biome tables, names, enchantments, permissions |
| `statistics.yml` | Persisted per-player counters. Don't hand-edit. |
| `lang/*.yml` | Translations. Disk overrides merge over bundled defaults. |

## `config.yml` defaults

```yaml
language:
  default: en_US   # en_US | en_GB | ja_JP (or any locale present in lang/)

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
    delivery-mode: vanilla_retrieve   # vanilla_retrieve | inventory | drop_at_player

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

## Behaviour notes

- `weight` is a relative roll weight, not a percentage.
- Rod luck multipliers only affect drops with `weight <= 5` (rare and
  legendary tiers).
- Invalid particle names fall back to safe defaults at startup or reload
  with a console warning.
- Negative or zero `weight` and out-of-range `amount` get clamped at load
  with a console warning that names the offending entry.

## `delivery-mode`

- `vanilla_retrieve` (default): replaces the caught entity in-flight so the
  reward arcs back to the player like a vanilla catch.
- `inventory`: inserts directly into the player's inventory.
- `drop_at_player`: drops the reward at the player's feet.

## Editing while the server is running

Two surfaces of MythicRod can be edited live without restarting:

- `drops.yml`: edit the file, run `/mythicrod validate`, then
  `/mythicrod reload`. Reload parses the new file into a temporary state
  first; a parse error leaves the previous valid state in place and prints
  the failure to console.
- `config.yml` reload picks up most flags. The bStats handshake is
  established once on enable; toggling it later does not unhook the
  reporter. A restart is the only reliable way to change bStats opt-in
  after first start.

`/mythicrod config language <locale>` is the supported way to change the
active language at runtime. Editing `language.default` in `config.yml`
takes effect on the next reload.

## Update checking

`features.update-check.enabled` (default `true`) controls a small async
worker that polls `api.github.com/repos/xcutiboo/MythicRod/releases/latest`
on enable and every six hours afterwards. The check sends no data; it only
parses the `tag_name` field and logs a single warning line when the
installed plugin is older than the latest published release. Setting the
flag to `false` stops every outbound call to GitHub from the plugin.

## What survives a reload, what does not

| Surface | Survives reload? |
|---|---|
| Open GUI menus | Yes; menus are invalidated and re-rendered with the new language. |
| Cached player stats | Yes. They flush to `statistics.yml` on shutdown and on the configured interval. |
| In-flight reward rolls | Yes. The reload swaps the drop table atomically, so a roll completes against either the old table or the new one but never against a mixed view. |
| Registered `ExternalDropProvider`s | Yes, until the registering plugin is itself disabled. |
| Custom bStats charts | Yes, but the values shown on bstats.org refresh on the bStats poll cadence rather than instantly. |

---

[← Back to docs home](./) · [GitHub](https://github.com/xcutiboo/MythicRod) · [Hangar](https://hangar.papermc.io/xcutiboo/MythicRod)
