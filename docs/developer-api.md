---
title: Developer API
nav_order: 8
has_children: true
---

# MythicRod Developer API
{: .no_toc }

A small, stable surface for integrating with MythicRod from your own Paper or
Folia plugin: query stats, inject drops, react to reward flow, build
MythicRod-compatible items, schedule work safely across Folia regions.

![divider]({{ site.baseurl }}/assets/divider-feature.svg)

## Table of contents
{: .no_toc .text-delta }

1. TOC
{:toc}

![divider]({{ site.baseurl }}/assets/divider.svg)

## At a glance

| Module             | Purpose                                                      |
| ------------------ | ------------------------------------------------------------ |
| `mythicrod-api`    | Stable contracts and platform-neutral DTOs                   |
| `mythicrod-paper`  | Paper runtime + the Paper-specific helper, events, listeners |
| `mythicrod-common` | Shared internals (do not depend on directly)                 |

**You almost always depend on `mythicrod-api` only.** Reach for
`mythicrod-paper` when you need the convenience helper
`MythicRodServices` or the three Bukkit events.

The base package is `io.xcutiboo.mythicrod.api.*` for stable types,
`io.xcutiboo.mythicrod.paper.api.*` for the Paper helper, and
`io.xcutiboo.mythicrod.paper.events.*` for the Bukkit events.

![divider]({{ site.baseurl }}/assets/divider.svg)

## Setup

### Gradle (Kotlin DSL)

```kotlin
repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.1.2.build.64-stable")
    // Compile against the MythicRod jar you ship alongside the runtime.
    compileOnly(files("libs/MythicRod-Paper-2026.1.0.jar"))
}
```

### Gradle (Groovy)

```groovy
dependencies {
    compileOnly 'io.papermc.paper:paper-api:26.1.2.build.64-stable'
    compileOnly files('libs/MythicRod-Paper-2026.1.0.jar')
}
```

### Maven

```xml
<dependencies>
  <dependency>
    <groupId>io.papermc.paper</groupId>
    <artifactId>paper-api</artifactId>
    <version>26.1.2.build.64-stable</version>
    <scope>provided</scope>
  </dependency>
  <dependency>
    <groupId>io.xcutiboo</groupId>
    <artifactId>mythicrod-paper</artifactId>
    <version>2026.1.0</version>
    <scope>system</scope>
    <systemPath>${project.basedir}/libs/MythicRod-Paper-2026.1.0.jar</systemPath>
  </dependency>
</dependencies>
```

> **Heads-up.** MythicRod does not yet publish a standalone Maven artifact. The
> jar that ships with each release contains both the API and the Paper runtime;
> use it as `compileOnly` / `provided` and never shade it.

### Composite or multi-project builds

```kotlin
dependencies {
    compileOnly(project(":mythicrod-api"))
    compileOnly(project(":mythicrod-paper")) // only for Paper events or MythicRodServices
}
```

### paper-plugin.yml load order

If your plugin resolves the MythicRod service at startup, declare MythicRod as
an optional, after-load server dependency:

```yaml
name: YourPlugin
main: example.YourPlugin
api-version: '1.21'
dependencies:
  server:
    MythicRod:
      load: AFTER
      required: false
```

Always handle the missing-service case cleanly when MythicRod is optional.

![divider]({{ site.baseurl }}/assets/divider.svg)

## Service lookup

Three entry points, all of them safe to call from `onEnable()` and beyond:

```java
import io.xcutiboo.mythicrod.api.MythicRodAPI;
import io.xcutiboo.mythicrod.paper.api.MythicRodServices;

// Required: throws IllegalStateException if MythicRod is not loaded.
MythicRodAPI api = MythicRodServices.require();

// Optional: empty Optional when MythicRod is missing.
MythicRodServices.find().ifPresent(found -> { /* ... */ });

// Server / ServicesManager overloads exist for test contexts.
MythicRodAPI test = MythicRodServices.require(Bukkit.getServer());
```

```kotlin
import io.xcutiboo.mythicrod.api.MythicRodAPI
import io.xcutiboo.mythicrod.paper.api.MythicRodServices

val api: MythicRodAPI = MythicRodServices.require()

MythicRodServices.find().ifPresent { /* ... */ }

val test = MythicRodServices.require(Bukkit.getServer())
```

If you'd rather not link `mythicrod-paper` at all, use the raw ServicesManager:

```java
RegisteredServiceProvider<MythicRodAPI> provider =
    Bukkit.getServicesManager().getRegistration(MythicRodAPI.class);
MythicRodAPI api = provider != null ? provider.getProvider() : null;
```

```kotlin
val api = Bukkit.getServicesManager().getRegistration(MythicRodAPI::class.java)?.provider
```

![divider]({{ site.baseurl }}/assets/divider.svg)

## Threading and Folia

| Surface                             | Where it runs                                            |
| ----------------------------------- | -------------------------------------------------------- |
| `MythicRodAPI#getPlayerStats(...)`  | Completes on a MythicRod-owned async thread              |
| `MythicRodAPI#getTopPlayers(...)`   | Completes on a MythicRod-owned async thread              |
| `MythicRodAPI#flushAllStats()`      | Completes on a MythicRod-owned async thread              |
| `ExternalDropProvider#getWeight`    | Fires from the fishing-event path (owner thread on Folia)|
| `ExternalDropProvider#generateItem` | Same as `getWeight`                                      |
| `MythicRodRewardRollEvent`          | Player-owner thread (Folia) or main thread (vanilla)     |
| `MythicRodFishCatchEvent`           | Player-owner thread (Folia) or main thread (vanilla)     |
| `MythicRodStatsUpdateEvent`         | MythicRod stats writer thread                            |
| `MythicRodReloadEvent`              | Thread that called `MythicRod#reload()` (main thread for `/mythicrod reload`) |

Rules of thumb:

- Treat every weight as a relative weight. **Not** a normalised percentage.
- Never block, sleep, or wait on a future inside `getWeight` / `generateItem`.
- After an async completion (CompletableFuture), reschedule back to the owner
  thread before mutating worlds, entities, inventories, or block state. Use
  `PlatformScheduler` or Folia's own schedulers.

![divider]({{ site.baseurl }}/assets/divider.svg)

## Stability map

| Package                              | Status              | Use it for                                                                          |
| ------------------------------------ | ------------------- | ----------------------------------------------------------------------------------- |
| `io.xcutiboo.mythicrod.api`          | Stable public API   | service interface, results, stat snapshots, external providers                      |
| `io.xcutiboo.mythicrod.api.drop`     | Stable public API   | read-only drop catalog inspection                                                   |
| `io.xcutiboo.mythicrod.api.platform` | Stable public API   | platform-neutral items, drops, players, schedulers, inventories, worlds, locations  |
| `io.xcutiboo.mythicrod.paper.api`    | Stable Paper helper | `MythicRodServices` convenience lookup, `PaperMythicRodAPI` runtime implementation  |
| `io.xcutiboo.mythicrod.paper.events` | Stable Paper events | reward-roll, reward-delivery, stats-update interception                             |
| `io.xcutiboo.mythicrod.drops`        | Internal type       | only when you intentionally need `CustomDrop`-specific behaviour through events     |
| anything else under `mythicrod-paper`| Internal            | not part of the contract, no compatibility guarantees                               |

![divider]({{ site.baseurl }}/assets/divider.svg)

## `MythicRodAPI` reference

The single service registered through Bukkit's `ServicesManager`. Every method
documented here is verified against
`mythicrod-api/src/main/java/io/xcutiboo/mythicrod/api/MythicRodAPI.java`.

### `String getVersion()`

The running plugin version (`"2026.1.0"`).

```java
getLogger().info("MythicRod " + api.getVersion());
```

```kotlin
logger.info("MythicRod ${api.version}")
```

### `DropCatalog getDropCatalog()`

A read-only snapshot view of the currently loaded drop table. See
[`DropCatalog`](#dropcatalog).

### `PlatformItemFactory getItemFactory()`

The runtime item factory MythicRod itself uses. Use this when you need to make
MythicRod-compatible items in your own code. See
[`PlatformItemFactory`](#platformitemfactory).

### `Result<PlatformItem> createItem(String identifier, int amount)`

Convenience wrapper around `getItemFactory().createItem(...)`.

```java
Result<PlatformItem> result = api.createItem("DIAMOND", 1);
if (result.isSuccess()) {
    PlatformItem item = result.getValue();
}
```

```kotlin
val result = api.createItem("DIAMOND", 1)
if (result.isSuccess) {
    val item = result.value
}
```

### `void registerExternalDropProvider(ExternalDropProvider provider)`

Adds your provider to MythicRod's weighted selection. Existing providers with
the same `getKey()` are replaced.

### `boolean unregisterExternalDropProvider(String key)`

Removes a previously registered provider. Returns `true` when a provider was
removed.

### `Optional<ExternalDropProvider> getExternalDropProvider(String key)`

Lookup a single provider by key.

### `List<ExternalDropProvider> getExternalDropProviders()`

Immutable snapshot of every registered provider. Useful for diagnostics.

### `CompletableFuture<PlayerStatSnapshot> getPlayerStats(UUID playerId)`

Async snapshot of a player's MythicRod stats. Completes with
`PlayerStatSnapshot.empty(uuid, "")` when MythicRod has never seen that player.
Cancellation aborts your continuation, not necessarily MythicRod's I/O.

### `CompletableFuture<List<PlayerStatSnapshot>> getTopPlayers(StatType type, int limit)`

Async sorted leaderboard. Limit is clamped to `1..100`. `StatType.LAST_FISHED`
is sorted most-recent-first; other types descending.

### `CompletableFuture<Void> flushAllStats()`

Forces an immediate persistence flush of in-memory stats. Called automatically
on plugin shutdown. Useful before server backups.

![divider]({{ site.baseurl }}/assets/divider.svg)

## `ExternalDropProvider`

Implement this to inject your own rewards into MythicRod's roll. All methods
in `ExternalDropProvider.java` are listed below.

| Method                                       | Required? | Notes                                                        |
| -------------------------------------------- | --------- | ------------------------------------------------------------ |
| `String getKey()`                            | Yes       | Stable namespaced key. Cannot change across restarts.        |
| `double getWeight(PlatformPlayer player)`    | Yes       | Relative weight. `<= 0` excludes the provider for that roll. |
| `PlatformItem generateItem(PlatformPlayer)`  | Yes       | Return `null` to abort delivery quietly.                     |
| `String getDisplayName()`                    | Default   | MiniMessage display name. Default: `<gray>Unknown Drop</gray>`. |
| `String getTier()`                           | Default   | Defaults to `"common"`. Use one of `common`, `uncommon`, `rare`, `legendary`, or a custom string. |

Full example:

```java
import io.xcutiboo.mythicrod.api.ExternalDropProvider;
import io.xcutiboo.mythicrod.api.MythicRodAPI;
import io.xcutiboo.mythicrod.api.platform.PlatformItem;
import io.xcutiboo.mythicrod.api.platform.PlatformPlayer;

public final class VipReward implements ExternalDropProvider {
    public static final String KEY = "myplugin:vip";

    private final MythicRodAPI api;

    public VipReward(MythicRodAPI api) { this.api = api; }

    @Override public String getKey() { return KEY; }

    @Override
    public double getWeight(PlatformPlayer player) {
        return player.hasPermission("myplugin.vip") ? 4.0D : 0.0D;
    }

    @Override
    public PlatformItem generateItem(PlatformPlayer player) {
        return api.createItem("DIAMOND", 1).orElse(null);
    }

    @Override public String getDisplayName() { return "<gold>VIP Diamond"; }
    @Override public String getTier() { return "rare"; }
}
```

```kotlin
import io.xcutiboo.mythicrod.api.ExternalDropProvider
import io.xcutiboo.mythicrod.api.MythicRodAPI
import io.xcutiboo.mythicrod.api.platform.PlatformItem
import io.xcutiboo.mythicrod.api.platform.PlatformPlayer

class VipReward(private val api: MythicRodAPI) : ExternalDropProvider {
    override fun getKey() = KEY
    override fun getWeight(player: PlatformPlayer) =
        if (player.hasPermission("myplugin.vip")) 4.0 else 0.0
    override fun generateItem(player: PlatformPlayer): PlatformItem? =
        api.createItem("DIAMOND", 1).orElse(null)
    override fun getDisplayName() = "<gold>VIP Diamond"
    override fun getTier() = "rare"
    companion object { const val KEY = "myplugin:vip" }
}
```

Lifecycle:

```java
@Override public void onEnable() {
    MythicRodServices.find().ifPresent(api -> api.registerExternalDropProvider(new VipReward(api)));
}

@Override public void onDisable() {
    MythicRodServices.find().ifPresent(api -> api.unregisterExternalDropProvider(VipReward.KEY));
}
```

```kotlin
override fun onEnable() {
    MythicRodServices.find().ifPresent { it.registerExternalDropProvider(VipReward(it)) }
}

override fun onDisable() {
    MythicRodServices.find().ifPresent { it.unregisterExternalDropProvider(VipReward.KEY) }
}
```

![divider]({{ site.baseurl }}/assets/divider.svg)

## Paper events

Three Bukkit events fire during the fishing pipeline. All live in
`io.xcutiboo.mythicrod.paper.events`.

### `MythicRodRewardRollEvent`

Fires once MythicRod has identified eligible drops but **before** it has built
the reward item. The right place to bias selection.

| Getter / setter           | Returns / accepts | Notes                                                    |
| ------------------------- | ----------------- | -------------------------------------------------------- |
| `getPlayer()`             | `Player`          | Paper player who triggered the roll                      |
| `getCategory()`           | `String`          | Category about to be rolled (e.g. `global`, `biome_ocean`)|
| `getBaseWeight()`         | `double`          | MythicRod's pre-bias weight scalar                       |
| `getLuckMultiplier()`     | `double`          | Current pending multiplier                               |
| `setLuckMultiplier(d)`    | `void`            | Override the multiplier for this roll                    |
| `forceDrop(CustomDrop)`   | `void`            | Force a specific drop (advanced; bypasses selection)     |
| `getForcedDrop()`         | `CustomDrop`      | Returns the forced drop if any (internal type)           |
| `getForcedDropView()`     | `PlatformDrop`    | Stable read-only view of the forced drop                 |
| `hasForcedDrop()`         | `boolean`         | True when a forced drop is set                           |
| `getHandlerList()` / `getHandlers()` | `HandlerList` | Standard Bukkit boilerplate                         |

```java
@EventHandler
public void onRoll(MythicRodRewardRollEvent event) {
    if (event.getPlayer().hasPermission("myplugin.lucky")) {
        event.setLuckMultiplier(event.getLuckMultiplier() * 1.5D);
    }
}
```

```kotlin
@EventHandler
fun onRoll(event: MythicRodRewardRollEvent) {
    if (event.player.hasPermission("myplugin.lucky")) {
        event.luckMultiplier = event.luckMultiplier * 1.5
    }
}
```

### `MythicRodFishCatchEvent`

Fires after selection. Cancellable. The reward item is mutable. Cancelling the
event leaves the vanilla catch in place.

| Getter / setter         | Returns / accepts | Notes                                                |
| ----------------------- | ----------------- | ---------------------------------------------------- |
| `getPlayer()`           | `Player`          | Paper player about to receive the catch              |
| `getDrop()`             | `CustomDrop`      | Internal MythicRod descriptor                         |
| `getDropView()`         | `PlatformDrop`    | Stable read-only view                                 |
| `getRewardItem()`       | `ItemStack`       | Item MythicRod is about to deliver                    |
| `setRewardItem(stack)`  | `void`            | Replace the reward item. Must not be `null` or `AIR`. |
| `isCancelled()` / `setCancelled(boolean)` | -   | Cancellable                                           |

```java
@EventHandler(ignoreCancelled = true)
public void onCatch(MythicRodFishCatchEvent event) {
    if ("legendary".equalsIgnoreCase(event.getDropView().getTier())) {
        Player p = event.getPlayer();
        p.getWorld().strikeLightningEffect(p.getLocation());
    }
}
```

```kotlin
@EventHandler(ignoreCancelled = true)
fun onCatch(event: MythicRodFishCatchEvent) {
    if (event.dropView.tier.equals("legendary", ignoreCase = true)) {
        val p = event.player
        p.world.strikeLightningEffect(p.location)
    }
}
```

### `MythicRodStatsUpdateEvent`

Fires after MythicRod has updated a player's stats. Read-only; useful for
analytics, dashboards, or webhooks.

| Getter           | Returns               | Notes                                          |
| ---------------- | --------------------- | ---------------------------------------------- |
| `getPlayerId()`  | `UUID`                | Player whose stats were updated                |
| `getTier()`      | `String`              | Tier of the catch that triggered the update    |
| `getSnapshot()`  | `PlayerStatSnapshot`  | Fresh, immutable stats snapshot               |

```java
@EventHandler
public void onStats(MythicRodStatsUpdateEvent event) {
    var snap = event.getSnapshot();
    Bukkit.getLogger().info(snap.playerName() + " total=" + snap.totalCaught());
}
```

```kotlin
@EventHandler
fun onStats(event: MythicRodStatsUpdateEvent) {
    val snap = event.snapshot
    Bukkit.getLogger().info("${snap.playerName()} total=${snap.totalCaught()}")
}
```

### `MythicRodReloadEvent`

Fires once `MythicRod#reload()` finishes. Not cancellable. Use it to refresh
your own caches that mirrored MythicRod state, or to log to a dashboard.

| Getter         | Returns   | Notes                                        |
| -------------- | --------- | -------------------------------------------- |
| `isSuccess()`  | `boolean` | `true` when reload completed without errors  |

```java
@EventHandler
public void onReload(MythicRodReloadEvent event) {
    if (event.isSuccess()) {
        myDropProviderCache.invalidate();
    }
}
```

```kotlin
@EventHandler
fun onReload(event: MythicRodReloadEvent) {
    if (event.isSuccess) {
        myDropProviderCache.invalidate()
    }
}
```

![divider]({{ site.baseurl }}/assets/divider.svg)

## Value types

### `Result<T>`

Tiny success/failure wrapper used by item creation. Static factories:
`Result.success(value)` / `Result.failure("reason")`. Instance methods:
`isSuccess()`, `isFailure()`, `getValue()`, `getError()`, `orElse(fallback)`,
`orElseThrow()`. `orElseThrow()` raises `IllegalStateException` with the stored
error message.

### `PlayerStatSnapshot`

Java record with these components, in declaration order:

| Component          | Type      | Meaning                                          |
| ------------------ | --------- | ------------------------------------------------ |
| `playerUuid`       | `UUID`    | Player UUID                                      |
| `playerName`       | `String`  | Last-known player name                           |
| `totalCaught`      | `int`     | Total custom catches                             |
| `commonCaught`     | `int`     | Common-tier catches                              |
| `uncommonCaught`   | `int`     | Uncommon-tier catches                            |
| `rareCaught`       | `int`     | Rare-tier catches                                |
| `legendaryCaught`  | `int`     | Legendary-tier catches                           |
| `basicRodUses`     | `int`     | Casts with a basic rod                           |
| `advancedRodUses`  | `int`     | Casts with an advanced rod                       |
| `legendaryRodUses` | `int`     | Casts with a legendary rod                       |
| `lastFished`       | `Instant` | Timestamp of last catch, `Instant.EPOCH` if none |
| `snapshotTime`     | `Instant` | When MythicRod built this snapshot               |

Static factory `PlayerStatSnapshot.empty(uuid, name)` returns a zeroed snapshot.

Nested enum `PlayerStatSnapshot.StatType` for `getTopPlayers(...)`:
`TOTAL_CAUGHT`, `RARE_CAUGHT`, `LEGENDARY_CAUGHT`, `LAST_FISHED`.

### `DropCatalog`

Read-only catalog of currently loaded drops.

| Method                | Returns                       | Notes                          |
| --------------------- | ----------------------------- | ------------------------------ |
| `getCategories()`     | `Set<String>`                 | Registered category keys       |
| `getDrops(category)`  | `List<? extends PlatformDrop>` | Drops for a category (snapshot) |
| `getAllDrops()`       | `List<? extends PlatformDrop>` | All loaded drops (snapshot)     |
| `getTotalDropCount()` | `int`                         | Count across all categories    |

```java
DropCatalog catalog = api.getDropCatalog();
for (String cat : catalog.getCategories()) {
    for (PlatformDrop drop : catalog.getDrops(cat)) {
        getLogger().info(cat + " -> " + drop.getIdentifier() + " (" + drop.getTier() + ")");
    }
}
```

```kotlin
val catalog = api.dropCatalog
catalog.categories.forEach { cat ->
    catalog.getDrops(cat).forEach { drop ->
        logger.info("$cat -> ${drop.identifier} (${drop.tier})")
    }
}
```

### `PlatformDrop`

Immutable view of a configured drop.

| Method            | Returns         | Notes                                                |
| ----------------- | --------------- | ---------------------------------------------------- |
| `getIdentifier()` | `String`        | Material or `nexo:...`                               |
| `getWeight()`     | `int`           | Relative roll weight                                 |
| `getAmount()`     | `int`           | Configured stack amount                              |
| `getTier()`       | `String`        | Default: `common`/`uncommon`/`rare`/`legendary` from weight |
| `isNexoItem()`    | `boolean`       | `true` when identifier targets Nexo                   |
| `getPermission()` | `String`        | Required permission, or `null` when unrestricted     |
| `getBiomes()`     | `List<String>`  | Biome constraints, or empty list when global         |
| `createItem()`    | `PlatformItem`  | May throw `UnsupportedOperationException` for descriptor-only entries |

### `PlatformItem`

Immutable item view.

| Method              | Returns                | Notes                                  |
| ------------------- | ---------------------- | -------------------------------------- |
| `getIdentifier()`   | `String`               | `DIAMOND` or `nexo:my_item`             |
| `getAmount()`       | `int`                  | Stack amount                            |
| `getDisplayName()`  | `String`               | Display name (may be `null`)            |
| `getLore()`         | `List<String>`         | Lore lines                              |
| `getEnchantments()` | `Map<String, Integer>` | Enchant key -> level                    |
| `getItemFlags()`    | `List<String>`         | Bukkit ItemFlag names                   |
| `isGlowing()`       | `boolean`              | Glint override on/off                   |
| `isCustom()`        | `boolean`              | True when sourced from a custom-item integration |

### `PlatformItemFactory`

| Method                                      | Returns                  | Notes                            |
| ------------------------------------------- | ------------------------ | -------------------------------- |
| `createItem(identifier, amount)`            | `Result<PlatformItem>`   | Use this rather than constructing items directly |
| `canCreate(identifier)`                     | `boolean`                | Quick probe                       |

### `PlatformPlayer` (extends `PlatformCommandSender`)

| Method               | Returns               | Notes                          |
| -------------------- | --------------------- | ------------------------------ |
| `getUniqueId()`      | `UUID`                | Stable UUID                     |
| `getName()`          | `String`              | Current player name             |
| `isOnline()`         | `boolean`             | Still connected?                 |
| `isOp()`             | `boolean`             | Operator status                  |
| `closeInventory()`   | `void`                | Closes the currently open inventory |
| `getInventory()`     | `PlatformInventory`   | Inventory view                   |

`PlatformCommandSender` also exposes `hasPermission(String)`, `sendMessage(String)`,
and friends used by integrations.

### `PlatformInventory`

| Method            | Returns                       | Notes                          |
| ----------------- | ----------------------------- | ------------------------------ |
| `getSize()`       | `int`                         | Slot count                      |
| `getTitle()`      | `String`                      | UI title or `null`              |
| `isFull()`        | `boolean`                     | Anything can fit?               |
| `addItem(item)`   | `Map<Integer, PlatformItem>`  | Overflow map keyed by slot index|
| `getItem(slot)`   | `PlatformItem`                | Returns `null` for empty slot    |

### `PlatformLocation` (record)

Components: `worldName`, `x`, `y`, `z`, `yaw`, `pitch`. Getter aliases
`getWorldName()`, `getX()`, `getY()`, `getZ()`, `getYaw()`, `getPitch()`.

### `PlatformWorld`, `PlatformServer`, `PlatformConfiguration`, `PlatformTask`

Smaller platform abstractions used by adapters and tests. Most consumer plugins
never touch them directly.

| Type                     | Use case                                                    |
| ------------------------ | ----------------------------------------------------------- |
| `PlatformWorld`          | World identity in tests and adapters                        |
| `PlatformServer`         | Host abstraction for the common module                      |
| `PlatformConfiguration`  | YAML adapter used by shared runtime code                    |
| `PlatformTask`           | Cancellable handle returned by `PlatformScheduler` delays   |

### `PlatformScheduler`

Folia-safe scheduling facade.

| Method                                                   | Returns / runs on                   |
| -------------------------------------------------------- | ----------------------------------- |
| `runAtLocation(loc, task)`                               | Region owner thread for that location |
| `runAtLocationDelayed(loc, task, delayTicks)`            | Same, returns `PlatformTask`         |
| `runForPlayer(player, task)`                             | Player owner thread                  |
| `runForPlayerDelayed(player, task, delayTicks)`          | Same, returns `PlatformTask`         |
| `runGlobal(task)`                                        | Global tick / scheduler              |
| `runGlobalDelayed(task, delayTicks)`                     | Same, returns `PlatformTask`         |
| `runGlobalRepeating(task, initialDelayTicks, periodTicks)`| Returns `PlatformTask`              |
| `runAsync(task)`                                         | Async thread                          |
| `runAsyncDelayed(task, delayTicks)`                      | Same, returns `PlatformTask`          |
| `runAsyncRepeating(task, initialMillis, periodMillis)`   | Returns `PlatformTask`; uses milliseconds |

![divider]({{ site.baseurl }}/assets/divider.svg)

## Recipes

### Award a bonus item when a legendary lands

```java
@EventHandler(ignoreCancelled = true)
public void onCatch(MythicRodFishCatchEvent e) {
    if (!"legendary".equalsIgnoreCase(e.getDropView().getTier())) return;
    e.getPlayer().getInventory().addItem(new ItemStack(Material.NETHER_STAR));
}
```

### Track every catch into your own analytics

```java
@EventHandler
public void onStats(MythicRodStatsUpdateEvent e) {
    var s = e.getSnapshot();
    metrics.record("mythicrod.catch", Map.of(
        "uuid", s.playerUuid().toString(),
        "tier", e.getTier(),
        "total", String.valueOf(s.totalCaught())
    ));
}
```

### Top-10 leaderboard sign refresh

```java
api.getTopPlayers(StatType.TOTAL_CAUGHT, 10).thenAccept(top -> {
    Bukkit.getScheduler().runTask(plugin, () -> updateSign(top));
});
```

```kotlin
api.getTopPlayers(StatType.TOTAL_CAUGHT, 10).thenAccept { top ->
    Bukkit.getScheduler().runTask(plugin) { updateSign(top) }
}
```

### Add a per-biome rare drop dynamically

```java
api.registerExternalDropProvider(new ExternalDropProvider() {
    @Override public String getKey() { return "myplugin:ocean_pearl"; }
    @Override public double getWeight(PlatformPlayer p) { return 2.0D; }
    @Override public PlatformItem generateItem(PlatformPlayer p) {
        return api.createItem("NAUTILUS_SHELL", 1).orElse(null);
    }
    @Override public String getTier() { return "rare"; }
});
```

![divider]({{ site.baseurl }}/assets/divider.svg)

## Contracts and pitfalls

- Weights are **relative**, not normalised percentages.
- `ExternalDropProvider#getWeight` and `generateItem` must stay non-blocking.
- `MythicRodFishCatchEvent#setRewardItem(...)` rejects `null` and `AIR`. Cancel
  the event if you want the vanilla catch instead.
- `MythicRodRewardRollEvent#forceDrop(...)` bypasses weight selection. Prefer
  `setLuckMultiplier(...)` for bias and let MythicRod pick.
- `PlatformDrop#createItem()` is not guaranteed to succeed for every drop
  descriptor; use `MythicRodAPI#getItemFactory()` when you need real items.
- Optional integrations such as `nexo:*` identifiers only work when that plugin
  is loaded at runtime.
- After any `CompletableFuture` completion, reschedule to the correct owner
  thread before touching Bukkit state.

![divider]({{ site.baseurl }}/assets/divider.svg)

## Versioning policy

MythicRod uses CalVer: `<year>.<release>.<patch>`. Patch releases never break
the API. Minor releases may add new methods with default implementations. Major
year-rollovers may rename or remove only when the changelog calls it out
explicitly.

The internal `CustomDrop` type and anything under
`io.xcutiboo.mythicrod.drops.*` is not covered by the stability guarantee.
Always prefer the platform-neutral views (`PlatformDrop`, `PlatformItem`,
`PlatformPlayer`) when you can.

![divider]({{ site.baseurl }}/assets/divider.svg)

[← Docs home]({{ site.baseurl }}/) · [GitHub](https://github.com/xcutiboo/MythicRod) ·
[Hangar](https://hangar.papermc.io/xcutiboo/MythicRod) ·
[Modrinth](https://modrinth.com/plugin/mythicrod)
