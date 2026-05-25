<div align="center">

<img src="assets/banner.svg" alt="MythicRod" width="640" />

[![Release](https://img.shields.io/github/v/release/xcutiboo/MythicRod?style=flat-square)](https://github.com/xcutiboo/MythicRod/releases) [![Paper 26.1.2](https://img.shields.io/badge/Paper-26.1.2-dca13a?style=flat-square)](https://papermc.io) [![Folia](https://img.shields.io/badge/Folia-region--ready-835516?style=flat-square)](https://papermc.io/software/folia) [![Discord](https://img.shields.io/badge/Discord-chat-5865F2?style=flat-square)](https://discord.gg/MDwtUgxX9U)

Weighted fishing drops with biome rules, permission gates, an in-game
drop editor, per-player statistics, and a small public API for Paper
and Folia.

</div>

<!-- HERO: 1280x640 lake render with a player landing a legendary catch. -->
<!-- ![Hero](assets/screenshots/hero.png) -->

## Features

- Weighted drop tables with biome and permission filters
- In-game GUI editor for drop entries (live, no reload)
- Per-player and global statistics plus a leaderboard
- Three rod tiers (basic / advanced / legendary) gated by permission
- Folia region-aware schedulers
- Public Java API for downstream plugins
- Crowdin localisation (English and Japanese ship in the jar)
- Optional Nexo integration for custom items

## Preview

<!-- Screenshots and short clips. PNG for stills, WebM or APNG for motion. -->
<!-- Suggested sizes: 1280×720 for full screens, 720×480 for tight crops. -->
<!-- Drop each file into assets/screenshots/ and uncomment the matching line. -->

<!-- Drop editor GUI (live edit, no reload): -->
<!-- ![Drop editor](assets/screenshots/editor.png) -->

<!-- Catch in action (rare drop landing, particles + sound): -->
<!-- ![Catch demo](assets/screenshots/catch.gif) -->

<!-- /mythicrod stats and the leaderboard view: -->
<!-- ![Stats screen](assets/screenshots/stats.png) -->

<!-- /mythicrod drops preview <biome> personalised by viewer perms: -->
<!-- ![Drops preview command](assets/screenshots/preview.png) -->

## Quick install

Drop the jar in `plugins/`, start the server, then `/mythicrod reload`
after editing `config.yml` and `drops.yml`.

## Documentation

Everything else lives on the docs site:
<https://xcutiboo.github.io/MythicRod/>

- Commands and permissions
- Configuration reference
- Drop table format
- Developer API
- Localisation workflow
- Release runbook

## Community

- [Discord](https://discord.gg/MDwtUgxX9U) for support and feature
  discussion.
- [Crowdin](https://crowdin.com/project/mythicrod) for translations.
- Issues and pull requests on
  [GitHub](https://github.com/xcutiboo/MythicRod).
