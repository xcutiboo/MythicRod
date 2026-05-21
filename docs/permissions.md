---
title: Permissions
nav_order: 4
---

# Permissions

| Permission                    | Default | Purpose                                             |
| ----------------------------- | ------- | --------------------------------------------------- |
| `mythicrod.command`           | `true`  | Base command access                                 |
| `mythicrod.gui`               | `true`  | Open the main GUI                                   |
| `mythicrod.stats.view`        | `true`  | View your own stats                                 |
| `mythicrod.stats.view.others` | `op`    | View another player's stats                         |
| `mythicrod.stats.leaderboard` | `true`  | View the leaderboard                                |
| `mythicrod.drops.view`        | `true`  | Browse drop tables                                  |
| `mythicrod.drops.global`      | `true`  | Receive drops from the default `global` category    |
| `mythicrod.drops.rare`        | `op`    | Receive drops from the default `rare` category      |
| `mythicrod.drops.legendary`   | `op`    | Receive drops from the default `legendary` category |
| `mythicrod.rod.advanced`      | `op`    | Use the Advanced rod tier                           |
| `mythicrod.rod.legendary`     | `op`    | Use the Legendary rod tier                          |
| `mythicrod.admin.reload`      | `op`    | Reload runtime data                                 |
| `mythicrod.admin.give`        | `op`    | Give MythicRod items                                |
| `mythicrod.admin.config`      | `op`    | Edit drops and config (incl. `stats reset`)         |
| `mythicrod.admin.debug`       | `op`    | Print debug information, run validate / testroll    |

Grouped trees: `mythicrod.*`, `mythicrod.admin.*`, `mythicrod.stats.*`,
`mythicrod.drops.*`, `mythicrod.rod.*` (all declared in `paper-plugin.yml`).

## Drop-level permission gates

Each individual drop entry under `drops.yml` may declare a `permission:`
field. When set, that drop only rolls for players who hold the permission
node. Use this to restrict cosmetic, event, or rank-locked rewards without
splitting them into their own category.
