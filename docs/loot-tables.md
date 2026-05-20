---
title: Loot tables
---

# Loot tables

`drops.yml` holds every reward MythicRod can deliver. Drops are grouped into
named categories. Three categories receive implicit defaults; everything else
is treated as a custom category.

## Example

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

## Drop fields

| Field               | Type                   | Meaning                                                        |
| ------------------- | ---------------------- | -------------------------------------------------------------- |
| `identifier`        | `String`               | Material name, `minecraft:*`, or `nexo:*` when Nexo is enabled |
| `weight`            | `int >= 1`             | Relative roll weight                                           |
| `amount`            | `int 1..64`            | Stack size                                                     |
| `custom_name`       | `String`               | MiniMessage display name                                       |
| `lore`              | `List<String>`         | MiniMessage lore lines                                         |
| `custom_model_data` | `int >= 0`             | Custom model data, `0` or omitted to disable                   |
| `glow`              | `boolean`              | Enchantment glow without a visible enchantment                 |
| `enchantments`      | `Map<String, Integer>` | Example: `'minecraft:unbreaking': 2`                           |
| `item_flags`        | `List<String>`         | Bukkit item flags such as `HIDE_ENCHANTS`                      |
| `biomes`            | `List<String>`         | Restrict a drop to specific biomes                             |
| `permission`        | `String`               | Permission node required to catch the drop                     |

## Implicit category behaviour

- `global` - base catches; permission gate `mythicrod.drops.global`.
- `rare` - uncommon/rare catches; permission gate `mythicrod.drops.rare`.
- `legendary` - top-tier catches; permission gate `mythicrod.drops.legendary`.

Categories named `biome_<name>` are automatically scoped to the matching
biome. `/mythicrod drops ocean` resolves to `biome_ocean`.

## Validation

Run `/mythicrod validate` after editing `drops.yml`. The output flags:

- Unknown materials.
- `nexo:*` identifiers when Nexo is not enabled.
- Weight or amount that fell outside the supported range (auto-clamped, but
  flagged so you can fix the source).
- Malformed or unknown enchantments.
- Unknown biome keys.
- Permissions outside the `mythicrod.*` namespace.
- Duplicate identifiers within a category.

## Tuning weights

Run `/mythicrod testroll <biome> [count]` to simulate up to 10,000 rolls in a
specific biome and print a tier histogram plus the top-five identifiers. The
simulation reuses the live loot table, so the output reflects the same odds
players experience.
