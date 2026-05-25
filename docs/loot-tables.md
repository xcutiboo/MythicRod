---
title: Loot tables
nav_order: 6
---

# Loot tables

![divider]({{ site.baseurl }}/assets/divider.svg)

`drops.yml` holds every reward MythicRod can deliver. Drops live inside
named categories. Three category names get implicit defaults; anything else
is just a custom category.

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

## Fields

| Field | Type | Meaning |
|---|---|---|
| `identifier` | `String` | Material name, `minecraft:*`, or `nexo:*` (Nexo enabled) |
| `weight` | `int >= 1` | Relative roll weight |
| `amount` | `int 1..64` | Stack size |
| `custom_name` | `String` | MiniMessage display name |
| `lore` | `List<String>` | MiniMessage lore |
| `custom_model_data` | `int >= 0` | Custom model data |
| `glow` | `boolean` | Glint without an actual enchant |
| `enchantments` | `Map<String, int>` | e.g. `'minecraft:unbreaking': 2` |
| `item_flags` | `List<String>` | Bukkit flags like `HIDE_ENCHANTS` |
| `biomes` | `List<String>` | Restrict to specific biomes |
| `permission` | `String` | Permission node required to catch |

## Implicit categories

- `global` -> `mythicrod.drops.global`
- `rare` -> `mythicrod.drops.rare`
- `legendary` -> `mythicrod.drops.legendary`

Categories named `biome_<name>` scope to the matching biome.
`/mythicrod drops ocean` resolves to `biome_ocean`.

## Validation

Run `/mythicrod validate` after editing. It flags:

- Unknown materials.
- `nexo:*` identifiers when Nexo isn't enabled.
- Weight or amount that fell outside the supported range (clamped at load,
  but still reported so you can fix the source).
- Malformed or unknown enchantments.
- Unknown biome keys.
- Permissions outside the `mythicrod.*` namespace.
- Duplicate identifiers within a category.

## Tuning weights

`/mythicrod testroll <biome> [count]` simulates up to 10,000 rolls in a
biome and prints a tier histogram plus the top-five identifiers. It uses
the live loot table, so the output reflects what players will see.

---

[← Back to docs home](./) · [GitHub](https://github.com/xcutiboo/MythicRod) · [Hangar](https://hangar.papermc.io/xcutiboo/MythicRod)
