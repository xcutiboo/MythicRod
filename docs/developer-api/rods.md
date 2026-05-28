# Rod identity

MythicRod rods are normal Paper `FISHING_ROD` items with two
PersistentDataContainer keys:

| Key | Type | Meaning |
| --- | --- | --- |
| `mythicrod:custom_rod` | byte (1) | This item is a MythicRod rod |
| `mythicrod:rod_tier` | string | One of `basic`, `advanced`, `legendary`, `mythic` |

The display name or lore is never used as identity. A survival player
cannot rename a vanilla rod into a MythicRod rod.

## Creating a rod from your plugin

The cleanest path is `MythicRodAPI.createRod(tier)`. It returns the
fully-tagged rod with display name, lore, glow, and unbreakable flag
matching MythicRod's built-in presets.

```java
MythicRodAPI api = MythicRodServices.require();
PlatformItem rod = api.createRod("advanced").orElseThrow();
player.getInventory().addItem(((PaperPlatformItem) rod).getItemStack());
```

Valid tiers (case-insensitive): `basic`, `advanced`, `legendary`,
`mythic`. An unknown tier returns a `Result.failure` rather than
throwing.

### Manual rod creation (legacy path)

If you need to override the preset, build the rod through the item
factory and write the PDC keys yourself:

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

Reach for the manual path only when the built-in preset does not fit;
otherwise prefer `createRod(tier)`.

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

[← Developer API](../developer-api.md)
