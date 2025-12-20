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

---

[← Back to docs home](./) · [GitHub](https://github.com/xcutiboo/MythicRod) · [Hangar](https://hangar.papermc.io/xcutiboo/MythicRod)
