---
title: Events
parent: Developer API
nav_order: 3
---

# Paper events

MythicRod publishes four Paper events. All live under
`io.xcutiboo.mythicrod.paper.events`.

| Event | Cancellable | Thread |
| --- | --- | --- |
| `MythicRodRewardRollEvent` | no | player-owner |
| `MythicRodFishCatchEvent` | yes | player-owner |
| `MythicRodStatsUpdateEvent` | no | stats writer |
| `MythicRodReloadEvent` | no | reload-caller |

## `MythicRodRewardRollEvent`

Fires once MythicRod has identified eligible drops but **before** it has
built the reward item. Bias selection here.

```java
@EventHandler
public void onRoll(MythicRodRewardRollEvent event) {
    if (event.getPlayer().hasPermission("myplugin.lucky")) {
        event.setLuckMultiplier(event.getLuckMultiplier() * 1.5D);
    }
}
```

Mutators: `setLuckMultiplier(double)`, `forceDrop(CustomDrop)`. The
forced-drop path is the advanced lane and bypasses weight selection;
prefer the multiplier when you can.

## `MythicRodFishCatchEvent`

Fires after selection. Cancellable. The reward `ItemStack` is mutable.
Cancelling leaves the vanilla catch in place.

```java
@EventHandler(ignoreCancelled = true)
public void onCatch(MythicRodFishCatchEvent event) {
    if ("legendary".equalsIgnoreCase(event.getDropView().getTier())) {
        event.getPlayer().getWorld().strikeLightningEffect(
            event.getPlayer().getLocation());
    }
}
```

`setRewardItem(ItemStack)` rejects `null` and `AIR`. Cancel the event
instead.

## `MythicRodStatsUpdateEvent`

Read-only. Useful for analytics, webhooks, or sign refreshes.

```java
@EventHandler
public void onStats(MythicRodStatsUpdateEvent event) {
    var snap = event.getSnapshot();
    metrics.record("mythicrod.catch", Map.of(
        "uuid", snap.playerUuid().toString(),
        "tier", event.getTier(),
        "total", String.valueOf(snap.totalCaught())
    ));
}
```

## `MythicRodReloadEvent`

Fires after `MythicRod#reload()` completes. Carries an `isSuccess()`
flag so listeners can refresh their caches only after a real swap.

```java
@EventHandler
public void onReload(MythicRodReloadEvent event) {
    if (event.isSuccess()) {
        myDropProviderCache.invalidate();
    }
}
```

## Threading and Folia

All four events fire on the same thread that owns the data they carry:

- Reward roll, fish catch: the player's owner thread on Folia, the main
  thread on vanilla Paper.
- Stats update: MythicRod's stats writer thread. Do not call Bukkit
  world or entity APIs inline.
- Reload: the thread that called `MythicRod#reload()`. For
  `/mythicrod reload` issued from the server console on Paper, that is
  the main thread. On Folia it is the global region scheduler.

A misbehaving listener cannot break the reload. Exceptions are caught
and logged by MythicRod.

## What not to do

- Do not mutate `event.getDrop()` directly. It is the internal type.
  Use `event.getDropView()` for inspection.
- Do not block, sleep, or wait on a future inside any event handler.
- Do not call `Bukkit.getScheduler()` to move entity-bound work back to
  the main thread on Folia. Use the Folia entity or region scheduler.

[← Developer API]({{ site.baseurl }}/developer-api/)
