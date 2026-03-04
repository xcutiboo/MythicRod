---
title: Home
nav_order: 1
permalink: /
---

<p align="center">
  <img src="{{ site.baseurl }}/assets/banner.svg" alt="MythicRod banner" width="720" />
</p>

<p align="center" markdown="1">
Weighted drop tables, biome filters, permission gates, in-game drop editor,
statistics, and a small public API.
</p>

<p align="center">
  <a href="https://github.com/xcutiboo/MythicRod"><img alt="GitHub" src="https://img.shields.io/badge/GitHub-xcutiboo%2FMythicRod-0b1320?style=flat-square&labelColor=0b1320&color=dca13a" /></a>
  <a href="https://hangar.papermc.io/xcutiboo/MythicRod"><img alt="Hangar" src="https://img.shields.io/badge/Hangar-listing-0b1320?style=flat-square&labelColor=0b1320&color=dca13a" /></a>
  <a href="https://crowdin.com/project/mythicrod"><img alt="Crowdin" src="https://img.shields.io/badge/Crowdin-translate-0b1320?style=flat-square&labelColor=0b1320&color=f0c75a" /></a>
  <a href="https://ko-fi.com/xcutiboo"><img alt="Ko-fi" src="https://img.shields.io/badge/Ko--fi-support-0b1320?style=flat-square&labelColor=0b1320&color=b87924" /></a>
</p>

![divider]({{ site.baseurl }}/assets/divider-feature.svg)

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
| [Testing checklist](testing.md) | 12-step Paper + Folia smoke test runbook. |
| [Release guide](release.md) | Secrets, pre-flight, tag flow, Hangar + Modrinth automation. |
| [Troubleshooting](troubleshooting.md) | Reload failures, Nexo, Folia notes. |

![divider]({{ site.baseurl }}/assets/divider.svg)

## Version targets

| Item | Value |
| --- | --- |
| Plugin | `2026.1.0` (CalVer, year.release.patch) |
| API | Paper `26.1.2` |
| Java | 25+ |
| Optional integration | Nexo (`nexo:*` identifiers) |
| Bundled languages | `en_US`, `ja_JP` (rest sync from Crowdin) |
| Scheduler | Paper-first, Folia owner-thread handoffs in place |

Folia hasn't been smoke-tested on a live build yet. The handoffs are written
into the listener, command, and scheduler paths, but treat the
`folia-supported: true` flag in the descriptor as a target until you've
exercised it on your own server.

![divider]({{ site.baseurl }}/assets/divider.svg)

## Status

- SonarCloud: 0 bugs / 0 vulnerabilities / 0 code smells / 0 hotspots.
- bStats: pluginId `23847` ([dashboard](https://bstats.org/plugin/bukkit/MythicRod/23847)).
- Crowdin: [crowdin.com/project/mythicrod](https://crowdin.com/project/mythicrod).
- Releases: [github.com/xcutiboo/MythicRod/releases](https://github.com/xcutiboo/MythicRod/releases).
