---
title: Drop providers
parent: Developer API
nav_order: 4
---

# External drop providers

`ExternalDropProvider` is how another plugin injects its own reward
rolls into MythicRod's selection pipeline.

## Implementation

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

## Lifecycle

```java
@Override public void onEnable() {
    MythicRodServices.find().ifPresent(api ->
        api.registerExternalDropProvider(new VipReward(api)));
}

@Override public void onDisable() {
    MythicRodServices.find().ifPresent(api ->
        api.unregisterExternalDropProvider(VipReward.KEY));
}
```

Register inside `onEnable()` after MythicRod's service is up. Always
use `find()` in `onDisable()` so your shutdown still completes when
MythicRod has already gone away.

## Method contracts

| Method | Required | Thread |
| --- | --- | --- |
| `getKey()` | yes | any |
| `getWeight(PlatformPlayer)` | yes | fishing-event path |
| `generateItem(PlatformPlayer)` | yes | fishing-event path |
| `getDisplayName()` | default | any |
| `getTier()` | default | any |

`getKey()` must be stable. The provider key is the durable identifier;
display name is presentation only.

`getWeight` runs on the player's owner thread (Folia) or the main
thread (vanilla Paper). Stay non-blocking. Returning `<= 0.0` means the
provider does not participate in that roll.

`generateItem` runs in the same thread context. Returning `null` aborts
the reward quietly and MythicRod will fall back to its built-in drop
table.

## Item creation

Always create reward items through the MythicRod factory:

```java
api.createItem("DIAMOND", 1)              // vanilla material
api.createItem("nexo:my_treasure", 1)     // Nexo-backed item
```

The factory returns a `Result<PlatformItem>` that you can `.orElse(null)`
or `.orElseThrow()`. Never build Paper `ItemStack` directly inside a
provider; that bypasses MythicRod's compatibility wrapper.

## Thread + Folia rules

- Do not call `Bukkit.getScheduler()` to leave the owner thread.
- Do not start a network request, database query, or anything blocking
  inside `getWeight` or `generateItem`. If you need that, pre-compute
  the value on a different thread and read it from a cache here.
- If you must mutate worlds, entities, or inventories from inside a
  provider, schedule the work through `PlatformScheduler.runForPlayer`
  or `runAtLocation` rather than calling Bukkit APIs inline.

## What not to do

- Do not expose your provider's internal state through `getKey()`. It
  must be stable.
- Do not mutate the `PlatformPlayer` you receive. It is an inspection
  view.
- Do not throw from `getWeight`. Any throwing provider is treated as
  inactive for that roll.

[← Developer API]({{ site.baseurl }}/developer-api/)
