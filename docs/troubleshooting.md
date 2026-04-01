---
title: Troubleshooting
nav_order: 10
---

# Troubleshooting

![divider]({{ site.baseurl }}/assets/divider.svg)

## Custom catches do nothing

- Confirm `features.permissions.enabled` matches your setup. With permissions
  enabled, players need at least `mythicrod.drops.global`.
- Run `/mythicrod validate`. The output names every disabled or malformed
  entry.
- Check `plugins/MythicRod/drops.yml` actually contains entries for the
  biome you're fishing in.

## A specific drop never appears

- Run `/mythicrod testroll <biome> 5000` and look at the histogram.
- Verify that the drop's `weight` is at least `1` (load-time clamping logs a
  warning when it isn't).
- Verify that the drop's `permission` (if any) is held by the player.
- Verify that the drop's `biomes:` list (if any) actually contains the
  player's current biome key.

## `nexo:*` items refuse to spawn

- Confirm Nexo is installed and enabled on the server.
- Confirm the identifier matches an existing Nexo item.
- `NexoItemProvider` consults `NexoItems.exists(id)` before resolving; check
  the server log for the corresponding warning.

## Reload feedback claims success but nothing changed

- Reload is single-flight. If a reload is already in progress, subsequent
  `/mythicrod reload` invocations report `already_running` rather than
  bouncing. Wait a second and retry.
- The reload swaps `DropManager`'s live drop table only after the new file
  parses cleanly. A parse error keeps the previous valid state in place; the
  exact error appears in the console.

## Folia stalls or region-thread warnings

- Inventory mutations, scheduled tasks, and entity calls all go through
  `PlatformScheduler`. If you see a region-thread warning, please open an
  issue with the stack trace.
- The plugin's `paper-plugin.yml` declares `folia-supported: true` but the
  current release has not yet been smoke-tested against a live Folia build.
  Validate before relying on Folia in production.

## bStats not reporting

- bStats setup is best-effort. A runtime issue during initialisation logs a
  warning and the plugin continues without metrics. Restart the server and
  check the log.

---

[← Back to docs home](./) · [GitHub](https://github.com/xcutiboo/MythicRod) · [Hangar](https://hangar.papermc.io/xcutiboo/MythicRod)
