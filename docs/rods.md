---
title: Custom rods
nav_order: 7
---

# Custom rods

![divider]({{ site.baseurl }}/assets/divider.svg)

Three tiers ship in the box: `basic`, `advanced`, `legendary`. Each is a
plain fishing rod with two PersistentDataContainer keys:

- `mythicrod:custom_rod` (`byte`, value `1`): marks the item as a MythicRod
  rod.
- `mythicrod:rod_tier` (`string`): tier name.

Advanced and legendary rods are crafted with the enchant glint flag set. The
legendary tier is also unbreakable so it can sit on a reward shelf without
needing repair.

Survival players can't fabricate these markers. Admins hand them out with
`/mythicrod give <player> <tier>`.

## Tier table

| Tier | Permission | Rare-luck multiplier |
|---|---|---|
| `basic` | (none) | default `1.00x`, configurable |
| `advanced` | `mythicrod.rod.advanced` | default `1.25x`, configurable |
| `legendary` | `mythicrod.rod.legendary` | default `1.50x`, configurable |

Multipliers only apply to drops with `weight <= 5` (rare and legendary
tiers). Common and uncommon catches stay unaffected.

## Inspecting a rod

`/mythicrod rod inspect` reads both hands and reports:

- Vanilla rod vs MythicRod marker.
- Tier stored in PDC.
- Rare-luck multiplier for that tier.

## Handing rods out

```
/mythicrod give <player> basic
/mythicrod give <player> advanced
/mythicrod give <player> legendary
```

On Folia, the actual `Inventory#addItem` runs on the target's owner
scheduler. If the target's inventory is full, the sender gets a clean
"inventory full" reply and no rod gets dropped on the ground.

## What "rod select" actually does

Selection sets a **per-player default tier**, not the item the player is
holding. Two cases:

1. The player casts with a **plain Minecraft fishing rod** (or any rod
   without MythicRod's PDC tier flag). The plugin reads the player's
   selected tier, checks they still hold the matching permission, and
   applies the rare-luck multiplier for that tier when rolling the catch.
2. The player casts with an **admin-issued MythicRod rod** (one carrying
   `mythicrod:custom_rod` and `mythicrod:rod_tier` in PDC). The held
   rod's PDC tier wins over the per-player default, provided the player
   still holds the matching permission. The held rod is the source of
   truth.

So `rod select advanced` does **not** modify the item in the player's
hand. It records "if you cast a vanilla rod, treat it as advanced." Lose
the permission later and the next cast quietly falls back to `basic`.

```text
/mythicrod rod select basic
/mythicrod rod select advanced
/mythicrod rod select legendary
```

The selection is stored on the player's PersistentDataContainer through
`PlayerDataService`. Selecting a tier the player lacks the permission for
is rejected with `command.rod.locked`. The GUI rod menu writes through
the same code path.

### When the held rod overrides the selection

| Held item PDC | Player has permission for held tier? | Effective tier |
|---|---|---|
| MythicRod tier `legendary` | yes | `legendary` |
| MythicRod tier `legendary` | no  | falls back to per-player default |
| Vanilla rod, no MythicRod PDC | n/a | per-player default |
| Per-player default unset | n/a | `basic` |

### Inspecting

`/mythicrod rod inspect` reads both hands and reports vanilla vs MythicRod
marker, the tier stored in PDC, and the rare-luck multiplier for that
tier. Use it when something feels off about a player's catches.

---

[← Back to docs home](./) · [GitHub](https://github.com/xcutiboo/MythicRod) · [Hangar](https://hangar.papermc.io/xcutiboo/MythicRod)
