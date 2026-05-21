---
title: Commands
nav_order: 3
---

# Commands

Aliases: `/mr`, `/mrod`.

| Command                                | Description                                       | Permission                          |
| -------------------------------------- | ------------------------------------------------- | ----------------------------------- |
| `/mythicrod`                           | Open the main GUI                                 | `mythicrod.command`                 |
| `/mythicrod gui`                       | Open the main GUI directly                        | `mythicrod.gui`                     |
| `/mythicrod rod`                       | Open rod and visual settings                      | `mythicrod.gui`                     |
| `/mythicrod rod inspect`               | Dump MythicRod metadata for the held rod          | `mythicrod.admin.debug`             |
| `/mythicrod reload`                    | Reload config, drops, players, and language files | `mythicrod.admin.reload`            |
| `/mythicrod stats [player]`            | View fishing statistics                           | `mythicrod.stats.view` (+ `.others`)|
| `/mythicrod stats reset <player>`      | Wipe a player's stats and persist the reset       | `mythicrod.admin.config`            |
| `/mythicrod top [limit]`               | View the leaderboard                              | `mythicrod.stats.leaderboard`       |
| `/mythicrod drops [category]`          | Browse drop tables or open a category directly    | `mythicrod.drops.view`              |
| `/mythicrod give <player> <tier>`      | Give a MythicRod item                             | `mythicrod.admin.give`              |
| `/mythicrod config [setting] [value]`  | View or edit core settings                        | `mythicrod.admin.config`            |
| `/mythicrod particle [channel] <type>` | Configure fishing particles                       | `mythicrod.admin.config`            |
| `/mythicrod validate`                  | Run a health check on loaded drops                | `mythicrod.admin.config`            |
| `/mythicrod testroll [biome] [count]`  | Simulate loot rolls and print a tier histogram    | `mythicrod.admin.debug`             |
| `/mythicrod debug`                     | Print debug info to console                       | `mythicrod.admin.debug`             |
| `/mythicrod help`                      | Show the command reference                        | `mythicrod.command`                 |

## Notes

- All subcommands are registered through Paper's Brigadier lifecycle, so tab
  completion automatically hides branches the sender cannot use.
- `/mythicrod give` dispatches inventory insertion on the target's owner
  scheduler. The command works correctly on both Paper and Folia.
- `/mythicrod validate` reports unknown materials, invalid weights and
  amounts, Nexo identifiers when Nexo is not enabled, malformed or unknown
  enchantments, unknown biome keys, permissions outside `mythicrod.*`, and
  duplicate identifiers within a category.
- `/mythicrod testroll` clamps `count` to `1..10000` and prints a tier
  histogram plus the top-five identifiers by frequency.
