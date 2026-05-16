---
title: Rods
parent: Developer API
nav_order: 7
---

# Rod identity

MythicRod rods are normal Paper `FISHING_ROD` items with two
PersistentDataContainer keys:

| Key | Type | Meaning |
| --- | --- | --- |
| `mythicrod:custom_rod` | byte (1) | This item is a MythicRod rod |
| `mythicrod:rod_tier` | string | One of `basic`, `advanced`, `legendary` |

The display name or lore is never used as identity. A survival player
cannot rename a vanilla rod into a MythicRod rod.

## Creating a rod from your plugin

If you want to hand out a MythicRod-compatible rod from your own code,
use the API's item factory instead of building an `ItemStack` by hand.
That route keeps your rod compatible with future internal changes:

```java
MythicRodAPI api = MythicRodServices.require();
ItemStack rod = api.createItem("FISHING_ROD", 1).orElseThrow().toItemStack();

ItemMeta meta = rod.getItemMeta();
NamespacedKey customRod = NamespacedKey.fromString("mythicrod:custom_rod");
NamespacedKey rodTier = NamespacedKey.fromString("mythicrod:rod_tier");
meta.getPersistentDataContainer().set(customRod, PersistentDataType.BYTE, (byte) 1);
meta.getPersistentDataContainer().set(rodTier, PersistentDataType.STRING, "advanced");
rod.setItemMeta(meta);
```

This is rare. Most integrations should hand off to MythicRod's own
`/mythicrod give` flow or use an `ExternalDropProvider` for tier-aware
rewards.

## Detecting a MythicRod rod

```java
NamespacedKey customRod = NamespacedKey.fromString("mythicrod:custom_rod");
NamespacedKey rodTier = NamespacedKey.fromString("mythicrod:rod_tier");

ItemStack held = player.getInventory().getItemInMainHand();
PersistentDataContainer pdc = held.getItemMeta().getPersistentDataContainer();

if (pdc.has(customRod, PersistentDataType.BYTE)) {
    String tier = pdc.get(rodTier, PersistentDataType.STRING);
    // tier ∈ {basic, advanced, legendary}
}
```

## Spoofing limits

- Players cannot write to a foreign plugin's namespace through any
  vanilla mechanism (anvil, command block, /give NBT, /attribute, etc.).
- A creative-mode admin with `/give` and raw NBT can craft a spoofed
  rod. Treat that as a server-trust boundary, not an MythicRod bug.
- A rod that loses its tier marker (server-side data corruption, manual
  PDC edit) is treated as the `basic` tier rather than as a custom rod.

## Compatibility with external item plugins

MythicRod rods are still vanilla `FISHING_ROD` items. They can carry
ItemsAdder/Oraxen/Nexo metadata in their own PDC namespace without
conflict. MythicRod ignores other namespaces and other plugins ignore
MythicRod's namespace.

## What not to do

- Do not rely on display name or lore to detect a MythicRod rod.
  Localised names break that check.
- Do not edit MythicRod's PDC keys from another plugin. Use
  `/mythicrod give` or expose your own integration through events.
- Do not assume the rod is always in main hand. Players may move it to
  off-hand or armor stand it.

[← Developer API]({{ site.baseurl }}/developer-api/)
