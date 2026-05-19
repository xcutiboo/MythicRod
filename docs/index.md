---
title: MythicRod
---

# MythicRod

Custom fishing progression for modern Paper servers. Weighted loot tables,
biome-aware drops, permission-gated rarities, statistics, in-game editing,
and a small public API.

## Quick links

- [Installation](installation.md)
- [Commands](commands.md)
- [Permissions](permissions.md)
- [Configuration reference](configuration.md)
- [Loot tables](loot-tables.md)
- [Custom rods](rods.md)
- [Developer API guide](developer-api.md)
- [Troubleshooting](troubleshooting.md)

## At a glance

| Topic                | Details                                       |
| -------------------- | --------------------------------------------- |
| Plugin version       | `2.0.0`                                       |
| Target platform      | Paper `26.1.2` (`api-version: 26.1.2`)        |
| Java runtime         | Java `25+`                                    |
| Optional integration | Nexo (`nexo:*` identifiers)                   |
| Bundled languages    | `en_US`, `ja_JP`                              |
| Scheduler model      | Paper-first with Folia-aware owner scheduling |

Folia owner handoffs are implemented but have not been smoke-tested against a
live Folia build. Validate on your target server before relying on Folia in
production.
