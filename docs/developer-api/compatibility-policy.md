---
title: Compatibility policy
parent: Developer API
nav_order: 6
---

# Compatibility policy

MythicRod uses CalVer: `<year>.<release>.<patch>`.

## What patch releases guarantee

A patch release (`2026.1.1` after `2026.1.0`) is **binary-compatible**.
You can drop the new jar onto a server without recompiling dependent
plugins. Bug fixes only.

## What minor releases guarantee

A minor release (`2026.2.0` after `2026.1.0`) adds new API methods only
with default implementations or new types. Existing types stay
binary-compatible. Recompilation is not required.

## What year roll-overs may change

A year roll-over (`2027.1.0` after `2026.x`) may rename or remove API
**only when the changelog calls it out explicitly**. Expect at least
one minor release marked deprecated before any removal.

## What lives outside the contract

- everything under `io.xcutiboo.mythicrod.drops.*`
- internal Paper runtime classes outside
  `io.xcutiboo.mythicrod.paper.api.*` and
  `io.xcutiboo.mythicrod.paper.events.*`
- field shapes on internal `CustomDrop`
- log message formatting
- locale key paths (those live under MythicRod's lang loader)

Pin against the stable surfaces:

| Stable | Internal |
| --- | --- |
| `io.xcutiboo.mythicrod.api` | `io.xcutiboo.mythicrod.drops` |
| `io.xcutiboo.mythicrod.api.drop` | `io.xcutiboo.mythicrod.text` |
| `io.xcutiboo.mythicrod.api.platform` | most of `mythicrod-paper` |
| `io.xcutiboo.mythicrod.paper.api` | |
| `io.xcutiboo.mythicrod.paper.events` | |

## Supported Paper API

| MythicRod | Paper API | Minecraft |
| --- | --- | --- |
| `2026.1.x` | `26.1.x` | `1.21.11` |

A Paper API generation bump always lands in a year roll-over or a
clearly-flagged minor release. Patch releases never raise the floor.

[← Developer API]({{ site.baseurl }}/developer-api/)
