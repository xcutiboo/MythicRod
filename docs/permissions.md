---
title: Permissions
nav_order: 4
---

# Permissions

| Node | Default | Purpose |
|---|---|---|
| `mythicrod.command` | `true` | Base command access |
| `mythicrod.gui` | `true` | Open the main GUI |
| `mythicrod.stats.view` | `true` | View your own stats |
| `mythicrod.stats.view.others` | `op` | View someone else's stats |
| `mythicrod.stats.leaderboard` | `true` | View the leaderboard |
| `mythicrod.drops.view` | `true` | Browse drop tables |
| `mythicrod.drops.global` | `true` | Receive `global` drops |
| `mythicrod.drops.rare` | `op` | Receive `rare` drops |
| `mythicrod.drops.legendary` | `op` | Receive `legendary` drops |
| `mythicrod.rod.advanced` | `op` | Use the advanced rod tier |
| `mythicrod.rod.legendary` | `op` | Use the legendary rod tier |
| `mythicrod.admin.reload` | `op` | Reload runtime data |
| `mythicrod.admin.give` | `op` | Give MythicRod items |
| `mythicrod.admin.config` | `op` | Edit drops, config, run `stats reset` |
| `mythicrod.admin.debug` | `op` | Run `validate` / `testroll` / `rod inspect` / `debug` |

Grouped parents (`mythicrod.*`, `mythicrod.admin.*`, `mythicrod.stats.*`,
`mythicrod.drops.*`, `mythicrod.rod.*`) are declared in `paper-plugin.yml`,
so giving an op the parent grants the whole tree.

## Drop-level gates

Each entry in `drops.yml` can declare its own `permission:` field. When set,
that single drop only rolls for players holding the node. Handy for
event-only or rank-locked rewards without splitting them out into their own
category.

---

[← Back to docs home](./) · [GitHub](https://github.com/xcutiboo/MythicRod) · [Hangar](https://hangar.papermc.io/xcutiboo/MythicRod)
