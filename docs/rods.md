---
title: Custom rods
nav_order: 7
---

# Custom rods

MythicRod ships three rod tiers: `basic`, `advanced`, and `legendary`. Each is
a regular fishing rod marked with two PersistentDataContainer keys:

- `mythicrod:custom_rod` - byte marker (`1`) that identifies the item as a
  MythicRod rod.
- `mythicrod:rod_tier` - string with the tier name.

Players cannot fabricate these markers in survival; admins distribute rods
through `/mythicrod give <player> <tier>`.

## Tier behaviour

| Tier        | Required permission         | Rare-luck multiplier |
| ----------- | --------------------------- | -------------------- |
| `basic`     | (none)                      | configurable, default `1.00x` |
| `advanced`  | `mythicrod.rod.advanced`    | configurable, default `1.25x` |
| `legendary` | `mythicrod.rod.legendary`   | configurable, default `1.50x` |

Multipliers apply only to drops with `weight <= 5` - that is the rare and
legendary tiers. Common/uncommon catches are unaffected.

## Inspecting metadata

`/mythicrod rod inspect` dumps the metadata for whichever rod the sender is
holding (main hand and off hand). Output covers:

- Vanilla rod vs. MythicRod marker.
- Tier stored in PDC.
- Configured rare-luck multiplier for that tier.

## Giving rods

```
/mythicrod give <player> basic
/mythicrod give <player> advanced
/mythicrod give <player> legendary
```

On Folia, the actual `Inventory#addItem` runs on the target's owner thread.
If the target's inventory is full, the rod is reported back to the sender
without dropping the item.
