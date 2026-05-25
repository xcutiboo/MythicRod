---
title: Home
nav_order: 1
permalink: /
---

<p align="center">
  <img src="../assets/icon-transparent.svg" alt="MythicRod icon" width="160" height="160" />
</p>

# MythicRod

<p align="center">
  <img src="../assets/banner.svg" alt="MythicRod banner" width="100%" />
</p>

Paper fishing plugin. Weighted drop tables, biome filters, permission gates,
stats, in-game drop editor, small public API.

![divider](../assets/divider.svg)

## Quick links

| Section | What it covers |
| --- | --- |
| [Installation](installation.md) | Drop-in setup, first reload, optional Nexo. |
| [Commands](commands.md) | Brigadier command reference and aliases. |
| [Permissions](permissions.md) | Node tree and per-drop gates. |
| [Configuration](configuration.md) | `config.yml` keys, defaults, messages. |
| [Loot tables](loot-tables.md) | `drops.yml` shape, biome categories, identifiers. |
| [Custom rods](rods.md) | Tier multipliers, in-game rod menu. |
| [Developer API](developer-api.md) | Service lookup, `ExternalDropProvider`, events. |
| [Troubleshooting](troubleshooting.md) | Reload failures, Nexo, Folia notes. |

![divider](../assets/divider.svg)

## Version targets

| Item | Value |
| --- | --- |
| Plugin | `26.5.0` (CalVer) |
| API | Paper `26.1.2` |
| Java | 25+ |
| Optional integration | Nexo (`nexo:*` identifiers) |
| Bundled languages | `en_US`, `ja_JP` (rest sync from Crowdin) |
| Scheduler | Paper-first, Folia owner-thread handoffs in place |

Folia hasn't been smoke-tested on a live build yet. The handoffs are written
into the listener, command, and scheduler paths, but treat the
`folia-supported: true` flag in the descriptor as a target until you've
exercised it on your own server.

![divider](../assets/divider.svg)

## Status

- SonarCloud: 0 bugs / 0 vulnerabilities / 0 code smells / 0 hotspots.
- bStats: pluginId `23847` ([dashboard](https://bstats.org/plugin/bukkit/MythicRod/23847)).
- Crowdin: [crowdin.com/project/mythicrod](https://crowdin.com/project/mythicrod).
- Releases: [github.com/xcutiboo/MythicRod/releases](https://github.com/xcutiboo/MythicRod/releases).
