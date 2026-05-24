# MythicRod Developer API

A small, stable Java surface for integrating with MythicRod from your own
Paper or Folia plugin. Add it as a `compileOnly` dependency, look up the
`MythicRodAPI` service, and call into it from the right scheduler.

The pages below cover everything in depth; this page is the index.

## Pages

- [Setup](developer-api/setup.md) - Gradle and Maven coordinates, `paper-plugin.yml` load order, multi-project notes.
- [Service access](developer-api/service-access.md) - look up `MythicRodAPI` at runtime and handle the not-installed case.
- [Events](developer-api/events.md) - the four MythicRod events and their typical use.
- [Drop providers](developer-api/drop-providers.md) - `ExternalDropProvider` SPI for registering custom items.
- [Folia threading](developer-api/folia-threading.md) - which scheduler each API call belongs on.
- [Rods](developer-api/rods.md) - rod tiers, identifiers, item factory.
- [Stats](developer-api/stats.md) - `PlayerStatSnapshot`, leaderboards, async behavior.
- [Examples](developer-api/examples.md) - short snippets for the most common integrations.
- [Compatibility policy](developer-api/compatibility-policy.md) - SemVer rules and what counts as a breaking change.

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
[Setup](developer-api/setup.md). The recommended
runtime lookup pattern is on
[Service access](developer-api/service-access.md).

## Stability

`mythicrod-api` follows semantic versioning. Breaking changes only land
on a major version. Methods marked `@ApiStatus.Experimental` may change
inside a major version. See
[Compatibility policy](developer-api/compatibility-policy.md).
