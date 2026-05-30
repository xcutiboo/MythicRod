# Changelog

Project history starts here. Earlier prototype builds are gone, so this file
is just the timeline going forward.

## 2026.1.0

Fresh start. The plugin was rebuilt from scratch as a multi-module project
around `mythicrod-api`, `mythicrod-common`, and `mythicrod-paper`. The Paper
runtime hosts everything live: Brigadier commands, GUIs, fishing listener,
scheduler bridge, and the optional Nexo integration. The API module is the
contract surface external plugins compile against. A small `mythicrod-spigot`
skeleton ships alongside so a Spigot jar exists for legacy servers (the
supported runtime is still Paper).

Highlights worth calling out:

- Drop tables are weighted, biome-scoped, and tier-aware. `drops.yml` is
  reloaded atomically and the live table swap is single-flight.
- Custom rods carry their tier in PersistentDataContainer, so server restarts
  and inventory moves keep the rod identity intact.
- Statistics are recorded per player, written through a save cadence you can
  tune, and visible in `/mythicrod stats` plus a leaderboard. Persistence
  uses `statistics.yml` so backups stay trivial.
- The GUI hub covers config toggles, drop edits, rod selection, stats, and a
  language picker that enumerates available locales at runtime.
- Folia is treated as a first target: the listener, scheduler service, and
  reward delivery path all hand off to the owning region thread instead of
  assuming a single main thread.
- A `MythicRodAPI` service is published through the Bukkit ServicesManager.
  External plugins can register weighted drop providers, query stats, or
  listen for the reward roll and catch events.
- Brigadier command suggestions are sourced from live state. You only see
  drop IDs, categories, biomes, and player names that actually exist on
  the server right now.
- `/mythicrod config` and `/mythicrod settings` (alias) cover the same edit
  surface: sounds, particles, statistics, biome-drops, permissions, debug,
  delivery-mode, language, and stats-save-interval. Changes write straight
  to `config.yml` and apply live.
- Bundled languages: `en_US` and `ja_JP`. Crowdin is wired up for the
  rest, and unknown locales fall back to a neutral skin in the language
  menu instead of breaking the layout.

If you tried earlier `1.0.x` builds, treat them as deleted. Versions reset
to a calendar scheme so the plugin version cannot be mistaken for a
Minecraft version. `2026.1.0` means: first cut of 2026.
