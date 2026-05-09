---
title: Service access
parent: Developer API
nav_order: 2
---

# Service access

MythicRod registers a single `MythicRodAPI` service through Bukkit's
`ServicesManager`. There are three entry points:

## Preferred Paper lookup

```java
import io.xcutiboo.mythicrod.api.MythicRodAPI;
import io.xcutiboo.mythicrod.paper.api.MythicRodServices;

MythicRodAPI api = MythicRodServices.require();
```

`require()` throws `IllegalStateException` if MythicRod is not loaded.
Use it inside `onEnable()` after the load-order dependency makes sure
MythicRod is ready.

## Optional lookup

```java
MythicRodServices.find().ifPresent(api -> {
    // integrate here
});
```

`find()` returns an `Optional`. Use it if MythicRod is a soft dependency
or in a shutdown path where MythicRod may already be disabled.

## Server / ServicesManager overloads

For tests or alternative entry points:

```java
MythicRodAPI api = MythicRodServices.require(Bukkit.getServer());
```

## Raw Bukkit lookup

If you do not link the Paper helper module:

```java
RegisteredServiceProvider<MythicRodAPI> p =
    Bukkit.getServicesManager().getRegistration(MythicRodAPI.class);
MythicRodAPI api = p != null ? p.getProvider() : null;
```

## Reload behaviour

`MythicRod#reload()` does not re-register the service. The same
`MythicRodAPI` instance survives reload. Listeners may want to react to
`MythicRodReloadEvent` to refresh their own caches.

## Plugin enable order

Declare `load: AFTER, required: false` on MythicRod in your plugin's
`paper-plugin.yml`. That guarantees the service is registered before
your `onEnable()` runs.

[← Developer API]({{ site.baseurl }}/developer-api/)
