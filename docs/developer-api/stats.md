# Stats API

MythicRod tracks per-player fishing statistics in
`plugins/MythicRod/statistics.yml`. Three methods on `MythicRodAPI`
expose them, and every call completes on a MythicRod-owned async
thread so the main thread is never blocked.

## What you get back

Each lookup returns a `PlayerStatSnapshot`. It's an immutable Java
record, safe to pass around and to log. Every field is filled in;
nothing is `null`.

| Field | Type | What it is |
| --- | --- | --- |
| `playerUuid()` | `UUID` | Player identity. Always present. |
| `playerName()` | `String` | Last name MythicRod saw for this UUID. Empty for unseen players. |
| `totalCaught()` | `int` | Total custom drops the player has caught. |
| `commonCaught()` | `int` | Catches that resolved to the `common` tier. |
| `uncommonCaught()` | `int` | Catches in the `uncommon` tier. |
| `rareCaught()` | `int` | Catches in the `rare` tier. |
| `legendaryCaught()` | `int` | Catches in the `legendary` tier. |
| `basicRodUses()` | `int` | Times the player has cast with a `basic` rod. |
| `advancedRodUses()` | `int` | Casts with an `advanced` rod. |
| `legendaryRodUses()` | `int` | Casts with a `legendary` rod. |
| `lastFished()` | `Instant` | When the player last caught anything. `Instant.EPOCH` if never. |
| `snapshotTime()` | `Instant` | When this snapshot was taken. |

All counter fields are `>= 0`; the record's constructor rejects
negative values, so you never have to defend against them.

## Reading a single player

```java
api.getPlayerStats(player.getUniqueId()).thenAccept(snap -> {
    int total       = snap.totalCaught();
    int rare        = snap.rareCaught();
    int legendary   = snap.legendaryCaught();
    Instant lastSeen = snap.lastFished();
    // snap.playerName(), snap.basicRodUses(), etc.
});
```

`getPlayerStats(UUID)` returns a `CompletableFuture<PlayerStatSnapshot>`.
For a UUID MythicRod has never seen, the future completes with
`PlayerStatSnapshot.empty(uuid, "")` rather than failing, so a missing
player is the same shape as a zeroed one.

## Leaderboards

```java
import io.xcutiboo.mythicrod.api.PlayerStatSnapshot.StatType;

api.getTopPlayers(StatType.TOTAL_CAUGHT, 10).thenAccept(rows -> {
    for (PlayerStatSnapshot row : rows) {
        String name  = row.playerName();
        int    total = row.totalCaught();
        Instant when = row.lastFished();
        // build leaderboard UI from here
    }
});
```

| `StatType` value | Sort order |
| --- | --- |
| `TOTAL_CAUGHT` | total catches, descending |
| `RARE_CAUGHT` | rare-tier catches, descending |
| `LEGENDARY_CAUGHT` | legendary-tier catches, descending |
| `LAST_FISHED` | most recent activity first |

`limit` is clamped to `1..100`. Ask for what you can render; a request
for `0` or `200` is silently rounded into range, never an error.

## Force a flush before a backup

```java
api.flushAllStats().thenRun(() -> {
    // Stats are now durable on disk; safe to snapshot statistics.yml.
});
```

MythicRod also flushes automatically on plugin shutdown and on the
configured cadence (`timers.stats-save-interval-seconds`). Manual
flushes are only useful right before backup snapshots or for tests.

## Reacting to updates as they happen

Polling works, but if you want a push, listen for
`MythicRodStatsUpdateEvent`. It carries the new snapshot and the tier
that triggered the change, so you can render leaderboards reactively
without busy-loops. See [events](events.md).

## Thread contract

Every future completes on a MythicRod-owned async thread. Before
touching Bukkit state from inside `thenAccept` / `thenRun`, hop back
to the owner thread:

```java
api.getPlayerStats(player.getUniqueId()).thenAccept(snap -> {
    player.getScheduler().run(plugin, task -> {
        player.sendMessage("You've caught " + snap.totalCaught() + " fish!");
    }, null);
});
```

See [Folia threading](folia-threading.md) for the full table of which
scheduler owns each Bukkit object.

## When the operator turns statistics off

When `features.statistics.enabled` is set to `false` in config:

- Catch events do not increment counters.
- `MythicRodStatsUpdateEvent` does not fire.
- `getPlayerStats(...)` and `getTopPlayers(...)` still return whatever
  was last persisted to disk; they do not throw.
- `flushAllStats()` is a no-op.

If your integration depends on live stats, log a friendly notice on
enable when the flag is off so the admin knows to flip it. Don't crash.

[← Developer API](../developer-api.md)
