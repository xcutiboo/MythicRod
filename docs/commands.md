# Commands

![divider](assets/divider.svg)

Aliases: `/mr`, `/mrod`. Brigadier-registered, so tab completion hides
branches the sender can't run.

| Command | What it does | Permission |
|---|---|---|
| `/mythicrod` / `/mythicrod gui` | Main GUI hub | `mythicrod.command` / `.gui` |
| `/mythicrod rod` | Visual + rod-tier settings | `mythicrod.gui` |
| `/mythicrod rod select <tier>` | Switch your own rod tier (`basic`, `advanced`, `legendary`) | `mythicrod.gui` (+ `.rod.advanced` / `.legendary`) |
| `/mythicrod rod inspect` | Dump PDC for the held rod | `mythicrod.admin.debug` |
| `/mythicrod drops [category]` | Drop browser, category view | `mythicrod.drops.view` |
| `/mythicrod drops preview <biome>` | Eligible drops + weight share for a biome | `mythicrod.admin.debug` |
| `/mythicrod effects [mode]` | Toggle or set visual effects (`normal`/`reduced`) for yourself | `mythicrod.gui` |
| `/mythicrod drop add <category> <identifier> <weight> <amount>` | Add drop and persist to `drops.yml` | `mythicrod.admin.config` |
| `/mythicrod drop remove <category> <identifier>` | Delete drop and persist | `mythicrod.admin.config` |
| `/mythicrod drop set <category> <identifier> <field> <value>` | Update one of `weight`, `amount`, `name`, `permission`, `glow` | `mythicrod.admin.config` |
| `/mythicrod stats [player]` | View stats | `mythicrod.stats.view` (+ `.others`) |
| `/mythicrod stats reset <player>` | Wipe a player's stats | `mythicrod.admin.config` |
| `/mythicrod top [limit]` | Leaderboard | `mythicrod.stats.leaderboard` |
| `/mythicrod give <player> <tier>` | Give a MythicRod | `mythicrod.admin.give` |
| `/mythicrod config [setting] [value]` | Runtime toggles (alias `/mythicrod settings`) | `mythicrod.admin.config` |
| `/mythicrod config language <locale>` | Switch the server language at runtime | `mythicrod.admin.config` |
| `/mythicrod particle [channel] <type>` | Particle config | `mythicrod.admin.config` |
| `/mythicrod validate` | Drop-config health check | `mythicrod.admin.config` |
| `/mythicrod testroll [biome] [count]` | Roll simulator + tier histogram | `mythicrod.admin.debug` |
| `/mythicrod reload` | Reload data atomically | `mythicrod.admin.reload` |
| `/mythicrod debug` | Console debug dump | `mythicrod.admin.debug` |
| `/mythicrod status` | Runtime snapshot (version, runtime mode, drops, locales, Nexo, stats) | `mythicrod.admin.debug` |
| `/mythicrod help` | Reference | `mythicrod.command` |

## Walkthrough: your first five minutes

1. Drop the jar into `plugins/` and start the server once. The plugin
   writes `plugins/MythicRod/config.yml`, `drops.yml`, `statistics.yml`,
   and `lang/`.
2. Run `/mythicrod status`. The output names the runtime (`Paper` or
   `Folia`), the active locale, and the drop count. If the line says
   `Drops: 0`, the config file is empty rather than broken; the default
   drops should have been written on first start, so check that the
   plugin folder isn't read-only.
3. Cast a fishing rod a few times to confirm the base catch flow works.
4. Open `/mythicrod gui` to browse drops and tiers.
5. Edit `drops.yml`, then run `/mythicrod validate` followed by
   `/mythicrod reload`. Validation flags errors before reload swaps the
   live table.

If anything in this sequence does not behave as described, check
[troubleshooting](troubleshooting.md) before opening
an issue.

## Behaviour worth knowing

- `/mythicrod give` runs the inventory insertion on the target's owner
  scheduler, so it's safe on Folia.
- `/mythicrod rod select` writes the tier to PersistentDataContainer
  through `PlayerDataService`, the same path the GUI rod menu uses. The
  permission rules are the same: anyone with `mythicrod.gui` may select
  `basic`; `mythicrod.rod.advanced` and `mythicrod.rod.legendary` gate the
  upper tiers.
- `/mythicrod drop add`, `remove`, and `set` persist immediately to
  `plugins/MythicRod/drops.yml`. The next `/mythicrod reload` is not
  required for the live drop table to reflect the change, but it is
  required if you edit the YAML by hand.
- `/mythicrod validate` flags unknown materials, weights or amounts out of
  range, `nexo:*` identifiers when Nexo isn't enabled, malformed or unknown
  enchantments, unknown biome keys, permissions outside the `mythicrod.*`
  namespace, and duplicate identifiers within a category.
- `/mythicrod testroll` clamps `count` to `1..10000` and prints a tier
  histogram plus the five most-frequent identifiers.
- `/mythicrod reload` parses the new files into a temporary state and only
  swaps the live drop table after the parse succeeds. A bad file leaves the
  previous state in place and prints the parse error to console.
- `/mythicrod config language` only accepts locales whose YAML file is on
  disk inside the plugin's `lang/` directory (`en_US`, `ja_JP`, and
  any Crowdin-synced files).

---

[← Back to docs home](./) · [GitHub](https://github.com/xcutiboo/MythicRod) · [Hangar](https://hangar.papermc.io/xcutiboo/MythicRod)
