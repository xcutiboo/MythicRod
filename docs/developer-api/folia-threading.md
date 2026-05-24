# Folia threading

MythicRod is Folia-safe out of the box. Plugins that integrate with it
must keep the same discipline.

## Owner thread concept

Every entity, world region, and player has a single thread that is
allowed to mutate its state. Folia rejects cross-thread mutation with
runtime exceptions. Paper has the same rule but is more forgiving:
violations there cause subtle data races instead of explicit crashes.

| Owner | Use this scheduler |
| --- | --- |
| a `Player` or `Entity` | entity scheduler (`runForPlayer`) |
| a `Location` or block | region scheduler (`runAtLocation`) |
| server-wide bookkeeping | global scheduler (`runGlobal`) |
| pure computation / blocking I/O | async scheduler (`runAsync`) |

`PlatformScheduler` is MythicRod's API-side facade; the runtime picks
the right Bukkit/Folia API for you.

## Where each API surface runs

| Surface | Thread |
| --- | --- |
| `MythicRodAPI.getPlayerStats(uuid)` | async completion |
| `MythicRodAPI.getTopPlayers(type, limit)` | async completion |
| `MythicRodAPI.flushAllStats()` | async completion |
| `ExternalDropProvider.getWeight` | player owner thread |
| `ExternalDropProvider.generateItem` | player owner thread |
| `MythicRodRewardRollEvent` | player owner thread |
| `MythicRodFishCatchEvent` | player owner thread |
| `MythicRodStatsUpdateEvent` | stats writer thread |
| `MythicRodReloadEvent` | reload-caller thread |

## Safe pattern

```java
api.getTopPlayers(StatType.TOTAL_CAUGHT, 10).thenAccept(top -> {
    // we are on a MythicRod async thread here.
    // schedule UI updates back to the owner.
    api.getScheduler().runForPlayer(viewer, () -> updateSign(top));
});
```

## Unsafe pattern

```java
api.getTopPlayers(StatType.TOTAL_CAUGHT, 10).thenAccept(top -> {
    viewer.sendMessage("..."); // BAD: async Bukkit call
    sign.setLine(0, "..."); // BAD: world mutation off-thread
});
```

## Rules of thumb

- Treat every `CompletableFuture` callback as async.
- After any async hop, schedule back to the correct owner before
  touching Bukkit state.
- Do not call `Bukkit.getScheduler()` from API code. It violates Folia
  region rules. Use `PlatformScheduler` instead.
- Do not block, sleep, or wait on a future inside an event handler or
  drop provider.
- Do not assume one global tick thread on Folia. There is none.

[← Developer API](../developer-api.md)
