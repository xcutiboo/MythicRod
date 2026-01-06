---
title: Developer API
nav_order: 8
---

# MythicRod Developer API Guide

![divider]({{ site.baseurl }}/assets/divider.svg)

MythicRod exposes a runtime service, Paper-specific event hooks, and a compact
set of platform-neutral value types so other plugins can integrate without
reaching into command, GUI, or fishing internals.

This guide is the developer-facing reference for that surface.

## Choose Your Surface

| Goal                             | Primary types                                                        | Module you need                    |
| -------------------------------- | -------------------------------------------------------------------- | ---------------------------------- |
| Resolve MythicRod at runtime     | `MythicRodAPI`, `MythicRodServices`                                  | `mythicrod-api`, `mythicrod-paper` |
| Inspect loaded rewards           | `DropCatalog`, `PlatformDrop`                                        | `mythicrod-api`                    |
| Create compatible reward items   | `MythicRodAPI#createItem`, `PlatformItemFactory`, `PlatformItem`     | `mythicrod-api`                    |
| Inject external rewards          | `ExternalDropProvider`                                               | `mythicrod-api`                    |
| Query stats and leaderboards     | `PlayerStatSnapshot`, `MythicRodAPI#getPlayerStats`, `getTopPlayers` | `mythicrod-api`                    |
| React to Paper reward flow       | `MythicRodRewardRollEvent`, `MythicRodFishCatchEvent`                | `mythicrod-paper`                  |
| Use stable read-only event drops | `PlatformDrop` via `getDropView()` and `getForcedDropView()`         | `mythicrod-api`, `mythicrod-paper` |

## Dependency Setup

`mythicrod-api` contains the stable service contracts and platform-neutral
views. `mythicrod-paper` adds the Paper convenience helper and the Paper events.

MythicRod does not currently publish a standalone Maven API artifact. Today,
external plugins compile against the MythicRod Paper jar and keep it
`compileOnly`.

### Composite or multi-project builds

```kotlin
dependencies {
    compileOnly(project(":mythicrod-api"))
    compileOnly(project(":mythicrod-paper")) // only if you need Paper events or MythicRodServices
}
```

### External plugins today

```kotlin
dependencies {
    compileOnly(files("libs/MythicRod-Paper-2026.1.0.jar"))
}
```

Important rules:

- Keep MythicRod `compileOnly`.
- Do not shade or relocate MythicRod classes into your own plugin.
- Prefer `io.xcutiboo.mythicrod.api.*` in your code and touch `io.xcutiboo.mythicrod.paper.*` only for the Paper helper or the Paper events.

### Runtime load order on Paper

If your plugin integrates with MythicRod at startup, declare it as an optional
Paper server dependency so the service is available before you resolve it.

```yaml
dependencies:
  server:
    MythicRod:
      load: AFTER
      required: false
```

If MythicRod is optional for your plugin, always handle the missing-service case
cleanly.

## Service Lookup

Preferred Paper lookup:

```java
import io.xcutiboo.mythicrod.api.MythicRodAPI;
import io.xcutiboo.mythicrod.paper.api.MythicRodServices;

MythicRodAPI api = MythicRodServices.require();
```

```kotlin
import io.xcutiboo.mythicrod.api.MythicRodAPI
import io.xcutiboo.mythicrod.paper.api.MythicRodServices

val api: MythicRodAPI = MythicRodServices.require()
```

Optional Paper lookup:

```java
MythicRodServices.find().ifPresent(api -> {
    // integrate here
});
```

```kotlin
MythicRodServices.find().ifPresent { api ->
    // integrate here
}
```

Manual Bukkit lookup:

```java
RegisteredServiceProvider<MythicRodAPI> provider =
    Bukkit.getServicesManager().getRegistration(MythicRodAPI.class);

if (provider != null) {
    MythicRodAPI api = provider.getProvider();
}
```

```kotlin
val provider = Bukkit.getServicesManager().getRegistration(MythicRodAPI::class.java)
val api = provider?.provider
```

## Threading And Folia Rules

- `MythicRodAPI#getPlayerStats`, `getTopPlayers`, and `flushAllStats` complete on MythicRod-owned async threads.
- `ExternalDropProvider` methods run on MythicRod's live fishing path and must stay fast and non-blocking.
- `MythicRodRewardRollEvent` and `MythicRodFishCatchEvent` fire on MythicRod's player-owned execution path. On ordinary Paper that is the synchronous event thread. On Folia it is the owning region thread.
- Do not mutate worlds, entities, or inventories from async completions unless you reschedule to the correct owner first.
- Treat all weight values as relative weights, not normalized percentages.

## Stability Map

| Package                              | Status                | Use it for                                                          |
| ------------------------------------ | --------------------- | ------------------------------------------------------------------- |
| `io.xcutiboo.mythicrod.api`          | Stable public API     | service lookup targets, results, stat snapshots, external providers |
| `io.xcutiboo.mythicrod.api.drop`     | Stable public API     | read-only drop catalog inspection                                   |
| `io.xcutiboo.mythicrod.api.platform` | Stable public API     | platform-neutral items, drops, players, schedulers, inventories     |
| `io.xcutiboo.mythicrod.paper.api`    | Stable Paper add-on   | `MythicRodServices` convenience lookup on Paper                     |
| `io.xcutiboo.mythicrod.paper.events` | Stable Paper add-on   | reward-roll and reward-delivery interception                        |
| `io.xcutiboo.mythicrod.drops`        | Advanced runtime type | only when you intentionally need `CustomDrop`-specific behavior     |

## Core Service Reference

| API surface                         | What it gives you                                            | Notes                                                         |
| ----------------------------------- | ------------------------------------------------------------ | ------------------------------------------------------------- |
| `MythicRodAPI#getVersion()`         | running plugin version string                                | useful for logging and compatibility checks                   |
| `MythicRodAPI#getDropCatalog()`     | snapshot view of the active loaded drop table                | read-only inspection surface                                  |
| `MythicRodAPI#getItemFactory()`     | runtime item factory                                         | supports vanilla and enabled integrations such as `nexo:*`    |
| `MythicRodAPI#createItem(...)`      | convenience item creation                                    | preferred over constructing platform implementations directly |
| `registerExternalDropProvider(...)` | injects weighted rewards into MythicRod's selection pipeline | provider keys replace on collision                            |
| `unregisterExternalDropProvider(key)` | removes a previously registered provider by key            | returns `true` when a provider was actually removed           |
| `getExternalDropProvider(key)`      | optional lookup for a single registered provider             | returns empty when no provider is registered for that key     |
| `getExternalDropProviders()`        | immutable snapshot of current providers                      | useful for diagnostics                                        |
| `getPlayerStats(UUID)`              | async single-player stats snapshot                           | completes on MythicRod async scheduler                        |
| `getTopPlayers(StatType, int)`      | async leaderboard snapshot                                   | limit is clamped to `1..100`                                  |
| `flushAllStats()`                   | async persistence flush                                      | useful before backups or controlled shutdown tasks            |

## Value Types And What They Mean

| Type                    | Purpose                                                      | Notes                                                                                                  |
| ----------------------- | ------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------ |
| `PlayerStatSnapshot`    | immutable stats snapshot returned by async API methods       | includes total catches, per-tier counts, rod usage, and last-fished time                               |
| `Result<T>`             | small success/failure wrapper used by item creation          | check `isSuccess()` before dereferencing the value                                                     |
| `DropCatalog`           | immutable inspection view of the active drop table           | category names plus loaded drops                                                                       |
| `PlatformDrop`          | read-only drop descriptor                                    | exposes identifier, weight, amount, tier, permission, biome filters, and optional item materialization |
| `PlatformItem`          | immutable MythicRod-compatible item view                     | stable metadata layer instead of Paper-specific item classes                                           |
| `PlatformPlayer`        | platform-neutral player context passed to external providers | identity, permissions, inventory, and basic state                                                      |
| `PlatformInventory`     | limited inventory abstraction                                | add items, inspect slots, and detect overflow/fullness                                                 |
| `PlatformLocation`      | immutable world-position snapshot                            | safe to move across scheduler boundaries                                                               |
| `PlatformScheduler`     | owner-aware scheduling facade                                | advanced integrations and test harnesses only                                                          |
| `PlatformServer`        | common-module host abstraction                               | mainly useful for adapters and tests, not normal Paper consumers                                       |
| `PlatformWorld`         | minimal world abstraction                                    | advanced integrations only                                                                             |
| `PlatformTask`          | cancel/is-cancelled handle                                   | returned by delayed and repeating scheduler work                                                       |
| `PlatformConfiguration` | configuration adapter used by shared runtime code            | mostly relevant for advanced adapters and tests                                                        |

Two important notes about `PlatformDrop`:

- `getTier()` returns MythicRod's rarity label using the same mapping MythicRod uses internally for reward statistics and event metadata.
- `createItem()` may throw `UnsupportedOperationException` because some drop descriptors are configuration-only views. Prefer `MythicRodAPI#createItem(...)` when you need a real item.

## Paper Events

| Event                      | Fires when                                                             | You can change                                         | Notes                                                                                          |
| -------------------------- | ---------------------------------------------------------------------- | ------------------------------------------------------ | ---------------------------------------------------------------------------------------------- |
| `MythicRodRewardRollEvent` | MythicRod has computed eligible rewards but has not built the item yet | `setLuckMultiplier(double)` or `forceDrop(CustomDrop)` | use `getForcedDropView()` for stable observation; `forceDrop(CustomDrop)` is the advanced path |
| `MythicRodFishCatchEvent`  | MythicRod has selected a reward and is about to deliver it             | `setRewardItem(ItemStack)` or `setCancelled(true)`     | cancellation keeps the original vanilla catch intact                                           |

For stable consumer code, prefer `getDropView()` from `MythicRodFishCatchEvent`
and `getForcedDropView()` from `MythicRodRewardRollEvent`. Those return
`PlatformDrop` instead of requiring `CustomDrop`-specific typing.

## Integration Recipes

### Read player stats

```java
MythicRodAPI api = MythicRodServices.require();

api.getPlayerStats(player.getUniqueId()).thenAccept(snapshot -> {
    getLogger().info(snapshot.playerName() + " has " + snapshot.totalCaught() + " custom catches");
});
```

```kotlin
val api = MythicRodServices.require()

api.getPlayerStats(player.uniqueId).thenAccept { snapshot ->
    logger.info("${snapshot.playerName()} has ${snapshot.totalCaught()} custom catches")
}
```

The completion runs async. Reschedule back to the correct player or region owner
before touching Bukkit state.

### Browse the loaded drop catalog

```java
DropCatalog catalog = api.getDropCatalog();

for (String category : catalog.getCategories()) {
    for (PlatformDrop drop : catalog.getDrops(category)) {
        getLogger().info(
            category + " -> " + drop.getIdentifier()
                + " [tier=" + drop.getTier()
                + ", weight=" + drop.getWeight()
                + ", amount=" + drop.getAmount() + "]"
        );
    }
}
```

### Create a MythicRod-compatible item

```java
Result<PlatformItem> result = api.createItem("nexo:my_reward", 1);
if (result.isSuccess()) {
    PlatformItem item = result.getValue();
    getLogger().info("Created " + item.getIdentifier());
} else {
    getLogger().warning(result.getError());
}
```

```kotlin
val result = api.createItem("nexo:my_reward", 1)
if (result.isSuccess) {
    val item = result.value
    logger.info("Created ${item.identifier}")
} else {
    logger.warning(result.error)
}
```

### Register an external drop provider

```java
public final class VipRewardProvider implements ExternalDropProvider {
    public static final String KEY = "myplugin:vip_reward";

    private final MythicRodAPI api;

    public VipRewardProvider(MythicRodAPI api) {
        this.api = api;
    }

    @Override
    public String getKey() {
        return KEY;
    }

    @Override
    public double getWeight(PlatformPlayer player) {
        return player.hasPermission("myplugin.vip") ? 3.0D : 0.0D;
    }

    @Override
    public PlatformItem generateItem(PlatformPlayer player) {
        return api.createItem("DIAMOND", 1).orElse(null);
    }

    @Override
    public String getDisplayName() {
        return "<gold>VIP Diamond</gold>";
    }

    @Override
    public String getTier() {
        return "rare";
    }
}
```

Registration lifecycle:

```java
MythicRodAPI api = MythicRodServices.require();
api.registerExternalDropProvider(new VipRewardProvider(api));
```

```kotlin
val api = MythicRodServices.require()
api.registerExternalDropProvider(VipRewardProvider(api))
```

Unregister the same key during your own disable phase when MythicRod is still
available:

```java
@Override
public void onDisable() {
    MythicRodServices.find().ifPresent(api ->
        api.unregisterExternalDropProvider(VipRewardProvider.KEY));
}
```

```kotlin
override fun onDisable() {
    MythicRodServices.find().ifPresent { api ->
        api.unregisterExternalDropProvider(VipRewardProvider.KEY)
    }
}
```

`MythicRodServices.find()` is used here instead of `require()` so your plugin's
shutdown still completes cleanly when MythicRod has already been disabled by
the server.

### React to reward flow on Paper

```java
@EventHandler(priority = EventPriority.NORMAL)
public void onRewardRoll(MythicRodRewardRollEvent event) {
    if (event.getPlayer().hasPermission("myplugin.vip")) {
        event.setLuckMultiplier(event.getLuckMultiplier() * 1.25D);
    }
}

@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
public void onFishCatch(MythicRodFishCatchEvent event) {
    PlatformDrop drop = event.getDropView();
    if ("legendary".equalsIgnoreCase(drop.getTier())) {
        ItemStack reward = event.getRewardItem();
        reward.setAmount(Math.min(reward.getMaxStackSize(), reward.getAmount() + 1));
        event.setRewardItem(reward);
    }
}
```

```kotlin
@EventHandler(priority = EventPriority.NORMAL)
fun onRewardRoll(event: MythicRodRewardRollEvent) {
    if (event.player.hasPermission("myplugin.vip")) {
        event.luckMultiplier = event.luckMultiplier * 1.25
    }
}

@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
fun onFishCatch(event: MythicRodFishCatchEvent) {
    val drop = event.dropView
    if (drop.tier.equals("legendary", ignoreCase = true)) {
        val reward = event.rewardItem
        reward.amount = minOf(reward.maxStackSize, reward.amount + 1)
        event.rewardItem = reward
    }
}
```

## Contract Notes And Pitfalls

- Weight values are relative weights, not percentages.
- `ExternalDropProvider#getWeight(...)` and `generateItem(...)` must not block, perform network I/O, or wait on futures.
- `MythicRodFishCatchEvent#setRewardItem(...)` rejects `null` and `AIR` items.
- `MythicRodFishCatchEvent#setCancelled(true)` skips MythicRod's custom replacement and keeps the original Minecraft catch intact.
- `MythicRodRewardRollEvent#forceDrop(CustomDrop)` is an advanced Paper-specific hook. Most plugins should prefer `setLuckMultiplier(...)` before selection or `setRewardItem(...)` after selection.
- Optional item integrations such as `nexo:*` only work when that plugin is actually enabled at runtime.
- `PlatformDrop#createItem()` is not guaranteed to succeed for every drop descriptor. Use the item factory when you need concrete item creation.

## What To Reach For First

If you are adding a normal MythicRod integration, start with this order:

1. Resolve `MythicRodAPI` through `MythicRodServices`.
2. Use `MythicRodAPI#createItem(...)` instead of platform-specific item builders.
3. Use `ExternalDropProvider` for new rewards.
4. Use `MythicRodRewardRollEvent` only when you need to bias selection.
5. Use `MythicRodFishCatchEvent` when you need to replace or veto the final reward item.

That path keeps your plugin on the stable surface and away from MythicRod's
internal runtime wiring.

---

[← Back to docs home](./) · [GitHub](https://github.com/xcutiboo/MythicRod) · [Hangar](https://hangar.papermc.io/xcutiboo/MythicRod)
