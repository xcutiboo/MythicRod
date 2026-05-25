---
title: Installation
nav_order: 2
---

# Installation

## What you need

- A Paper server matching the `paperVersion` in `gradle.properties`
  (currently Paper `26.1.2`, Minecraft 1.21.x).
- Java 25 or newer on the server host.
- Optional: [Nexo](https://polymart.org/resource/nexo) if you want
  `nexo:*` identifiers in `drops.yml`.

## From a release jar

1. Grab `MythicRod-Paper-<version>.jar` from the
   [releases page](https://github.com/xcutiboo/MythicRod/releases).
2. Drop it in `plugins/`.
3. Start the server once. MythicRod generates `plugins/MythicRod/config.yml`,
   `drops.yml`, `statistics.yml`, and `lang/*.yml`.
4. Edit `config.yml` and `drops.yml`.
5. `/mythicrod reload` after changes.

## From source

```bash
git clone https://github.com/xcutiboo/MythicRod.git
cd MythicRod
./gradlew :mythicrod-paper:shadowJar
```

Shaded jar lands at `mythicrod-paper/build/libs/MythicRod-Paper-<version>.jar`.

## Verifying release artefacts

Tagged releases ship `MythicRod-Paper-<version>.jar.sha256` next to the jar:

```bash
sha256sum -c MythicRod-Paper-26.5.0.jar.sha256
```

`v*-rc*`, `v*-beta*`, `v*-alpha*`, `v*-snapshot*` tags publish as
pre-releases automatically.

---

[← Back to docs home](./) · [GitHub](https://github.com/xcutiboo/MythicRod) · [Hangar](https://hangar.papermc.io/xcutiboo/MythicRod)
