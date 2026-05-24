# Stats API

MythicRod tracks per-player fishing statistics in
`plugins/MythicRod/statistics.yml`. Access them through three methods
on `MythicRodAPI`.

## Read a single player's snapshot

```java
api.getPlayerStats(player.getUniqueId()).thenAccept(snap -> {
    long total = snap.totalCaught();
    int rare = snap.rareCaught();
    int legendary = snap.legendaryCaught();
});
```

`getPlayerStats(UUID)` returns a `CompletableFuture<PlayerStatSnapshot>`.
For an unknown UUID, the future completes with
`PlayerStatSnapshot.empty(uuid, "")` rather than failing.

## Leaderboards

```java
import io.xcutiboo.mythicrod.api.PlayerStatSnapshot.StatType;

api.getTopPlayers(StatType.TOTAL_CAUGHT, 10).thenAccept(top -> {
    for (PlayerStatSnapshot row : top) {
        // row.playerName(), row.totalCaught(), row.lastFished()
    }
});
```

| `StatType` | Sort order |
| --- | --- |
| `TOTAL_CAUGHT` | total catches, descending |
| `RARE_CAUGHT` | rare catches, descending |
| `LEGENDARY_CAUGHT` | legendary catches, descending |
| `LAST_FISHED` | most-recent catch first |

`limit` is clamped to `1..100`.

## Force a flush

```java
api.flushAllStats().thenRun(() -> {
    // Stats are now durable on disk.
});
```

Useful right before a backup. MythicRod also flushes automatically on
plugin shutdown and on the configured cadence
(`timers.stats-save-interval-seconds`).

## Stat updates as events

If you need a push notification rather than a poll, listen for
`MythicRodStatsUpdateEvent`. It carries the updated snapshot and the
catch tier that triggered the update. See
[events](events.md).

## Thread contract

All three methods complete on a MythicRod-owned async thread. Schedule
back to the right owner before touching Bukkit state. See
[Folia threading](folia-threading.md).

## What `statistics: false` does

When the `features.statistics.enabled` config flag is off:

- Catch events do not increment counters.
- `MythicRodStatsUpdateEvent` does not fire.
- `getPlayerStats(...)` and `getTopPlayers(...)` still return whatever
  was last persisted to disk. They do not error out.
- `flushAllStats()` is a no-op.

Treat statistics as opt-in for admins. Plugins that rely on them should
either degrade gracefully or warn the operator when statistics are
disabled.

[← Developer API](../developer-api.md)
