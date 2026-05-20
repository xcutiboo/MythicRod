---
title: Configuration
---

# Configuration

MythicRod ships with three runtime files inside `plugins/MythicRod/`:

| File              | What it holds                                                          |
| ----------------- | ---------------------------------------------------------------------- |
| `config.yml`      | Feature toggles, particle names, language, rod multipliers, intervals |
| `drops.yml`       | Drop categories, biome tables, custom names, enchantments, permissions |
| `statistics.yml`  | Persisted per-player counters (managed by the plugin; do not hand-edit)|
| `lang/*.yml`      | Translation files. Disk overrides merge over bundled defaults.         |

## `config.yml`

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

### Notes

- `weight` is a relative roll weight, not a percentage.
- Rod luck multipliers affect only weights `<= 5` (rare and legendary tiers).
- Invalid particle names are corrected to safe defaults at startup or reload.
- Negative or zero `weight` and out-of-range `amount` values are clamped at
  load with a console warning that names the offending drop.
- `delivery-mode`:
  - `vanilla_retrieve` (default) - replaces the caught entity in-flight so
    the reward arcs back to the player like a vanilla catch.
  - `inventory` - inserts directly into the player's inventory.
  - `drop_at_player` - drops the reward at the player's feet.
