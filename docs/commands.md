---
title: Commands
nav_order: 3
---

# Commands

![divider]({{ site.baseurl }}/assets/divider.svg)

Aliases: `/mr`, `/mrod`. Brigadier-registered, so tab completion hides
branches the sender can't run.

| Command | What it does | Permission |
|---|---|---|
| `/mythicrod` / `/mythicrod gui` | Main GUI hub | `mythicrod.command` / `.gui` |
| `/mythicrod rod` | Visual + rod-tier settings | `mythicrod.gui` |
| `/mythicrod rod inspect` | Dump PDC for the held rod | `mythicrod.admin.debug` |
| `/mythicrod drops [category]` | Drop browser, category view | `mythicrod.drops.view` |
| `/mythicrod stats [player]` | View stats | `mythicrod.stats.view` (+ `.others`) |
| `/mythicrod stats reset <player>` | Wipe a player's stats | `mythicrod.admin.config` |
| `/mythicrod top [limit]` | Leaderboard | `mythicrod.stats.leaderboard` |
| `/mythicrod give <player> <tier>` | Give a MythicRod | `mythicrod.admin.give` |
| `/mythicrod config [setting] [value]` | Runtime toggles | `mythicrod.admin.config` |
| `/mythicrod particle [channel] <type>` | Particle config | `mythicrod.admin.config` |
| `/mythicrod validate` | Drop-config health check | `mythicrod.admin.config` |
| `/mythicrod testroll [biome] [count]` | Roll simulator + tier histogram | `mythicrod.admin.debug` |
| `/mythicrod reload` | Reload data atomically | `mythicrod.admin.reload` |
| `/mythicrod debug` | Console debug dump | `mythicrod.admin.debug` |
| `/mythicrod help` | Reference | `mythicrod.command` |

## Behaviour worth knowing

- `/mythicrod give` runs the inventory insertion on the target's owner
  scheduler, so it's safe on Folia.
- `/mythicrod validate` flags unknown materials, weights or amounts out of
  range, `nexo:*` identifiers when Nexo isn't enabled, malformed or unknown
  enchantments, unknown biome keys, permissions outside the `mythicrod.*`
  namespace, and duplicate identifiers within a category.
- `/mythicrod testroll` clamps `count` to `1..10000` and prints a tier
  histogram plus the five most-frequent identifiers.
- `/mythicrod reload` parses the new files into a temporary state and only
  swaps the live drop table after the parse succeeds. A bad file leaves the
  previous state in place and prints the parse error to console.

---

[← Back to docs home](./) · [GitHub](https://github.com/xcutiboo/MythicRod) · [Hangar](https://hangar.papermc.io/xcutiboo/MythicRod)
