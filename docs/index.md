---
title: Home
nav_order: 1
permalink: /
---

# MythicRod

Paper fishing plugin. Weighted drop tables, biome filters, permission gates,
stats, in-game drop editor, small public API.

## Pages

- [Installation](installation.md)
- [Commands](commands.md)
- [Permissions](permissions.md)
- [Configuration](configuration.md)
- [Loot tables](loot-tables.md)
- [Custom rods](rods.md)
- [Developer API](developer-api.md)
- [Troubleshooting](troubleshooting.md)

## Version targets

| Item | Value |
|---|---|
| Plugin | `26.5.0` (CalVer) |
| API | Paper `26.1.2` |
| Java | 25+ |
| Optional integration | Nexo (`nexo:*` identifiers) |
| Bundled languages | `en_US`, `ja_JP` (rest sync from Crowdin) |
| Scheduler | Paper-first, Folia owner-thread handoffs in place |

Folia hasn't been smoke-tested on a live build yet. The handoffs are written
into the listener / command / scheduler paths but treat the
`folia-supported: true` flag in the descriptor as a target until you've
exercised it on your own server.
