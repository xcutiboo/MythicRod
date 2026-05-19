---
title: Installation
---

# Installation

## Requirements

- A Paper server matching the `paperVersion` declared in `gradle.properties`
  (currently Paper `26.1.2`, Minecraft 1.21.x).
- Java 25 or newer.
- (Optional) [Nexo](https://polymart.org/resource/nexo) if you intend to use
  `nexo:*` item identifiers in your loot tables.

## From a release jar

1. Download the latest `MythicRod-Paper-x.y.z.jar` from the
   [Releases page](https://github.com/xcutiboo/MythicRod/releases).
2. Drop it into your server's `plugins/` directory.
3. Start the server once. MythicRod generates `plugins/MythicRod/config.yml`,
   `plugins/MythicRod/drops.yml`, `plugins/MythicRod/statistics.yml`, and
   `plugins/MythicRod/lang/*.yml`.
4. Edit `config.yml` and `drops.yml` as needed.
5. Run `/mythicrod reload` after configuration changes.

## From source

```bash
git clone https://github.com/xcutiboo/MythicRod.git
cd MythicRod
./gradlew :mythicrod-paper:shadowJar
```

The shaded jar lands at `mythicrod-paper/build/libs/MythicRod-Paper-x.y.z.jar`.

## Release artefact integrity

Tagged releases ship with `MythicRod-Paper-x.y.z.jar.sha256` alongside each
jar. Verify before deploying to production:

```bash
sha256sum -c MythicRod-Paper-2.0.0.jar.sha256
```
