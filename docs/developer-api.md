---
title: Developer API
nav_order: 8
has_children: true
---

# MythicRod Developer API

A small, stable Java surface for integrating with MythicRod from your own
Paper or Folia plugin. Add it as a `compileOnly` dependency, look up the
`MythicRodAPI` service, and call into it from the right scheduler.

The pages below cover everything in depth; this page is the index.

## Pages

- [Setup]({{ site.baseurl }}/developer-api/setup.html) - Gradle and Maven coordinates, `paper-plugin.yml` load order, multi-project notes.
- [Service access]({{ site.baseurl }}/developer-api/service-access.html) - look up `MythicRodAPI` at runtime and handle the not-installed case.
- [Events]({{ site.baseurl }}/developer-api/events.html) - the four MythicRod events and their typical use.
- [Drop providers]({{ site.baseurl }}/developer-api/drop-providers.html) - `ExternalDropProvider` SPI for registering custom items.
- [Folia threading]({{ site.baseurl }}/developer-api/folia-threading.html) - which scheduler each API call belongs on.
- [Rods]({{ site.baseurl }}/developer-api/rods.html) - rod tiers, identifiers, item factory.
- [Stats]({{ site.baseurl }}/developer-api/stats.html) - `PlayerStatSnapshot`, leaderboards, async behavior.
- [Examples]({{ site.baseurl }}/developer-api/examples.html) - short snippets for the most common integrations.
- [Compatibility policy]({{ site.baseurl }}/developer-api/compatibility-policy.html) - SemVer rules and what counts as a breaking change.

## Module layout

| Module | Purpose |
| --- | --- |
| `mythicrod-api` | Stable contracts and platform-neutral DTOs. Depend on this. |
| `mythicrod-paper` | Paper runtime and helpers. Available at runtime, do not depend on. |
| `mythicrod-common` | Shared internals. Not part of the API. |

## Quick start

```kotlin
dependencies {
    compileOnly("io.xcutiboo:mythicrod-api:VERSION")
}
```

```java
MythicRodAPI api = Bukkit.getServicesManager().load(MythicRodAPI.class);
if (api == null) {
    return; // MythicRod is not installed; skip integration.
}
```

Full setup, the Maven block, and `paper-plugin.yml` load order are on
[Setup]({{ site.baseurl }}/developer-api/setup.html). The recommended
runtime lookup pattern is on
[Service access]({{ site.baseurl }}/developer-api/service-access.html).

## Stability

`mythicrod-api` follows semantic versioning. Breaking changes only land
on a major version. Methods marked `@ApiStatus.Experimental` may change
inside a major version. See
[Compatibility policy]({{ site.baseurl }}/developer-api/compatibility-policy.html).
