<p align="center">
  <img src="assets/banner.svg" alt="MythicRod banner" width="720" />
</p>

<p align="center" markdown="1">
Weighted drop tables, biome filters, permission gates, in-game drop editor,
statistics, and a small public API. Paper-first, Folia-verified.
</p>

<p align="center">
  <a href="https://github.com/xcutiboo/MythicRod"><img alt="GitHub" src="https://img.shields.io/badge/GitHub-xcutiboo%2FMythicRod-0b1320?style=flat-square&labelColor=0b1320&color=dca13a" /></a>
  <a href="https://hangar.papermc.io/xcutiboo/MythicRod"><img alt="Hangar" src="https://img.shields.io/badge/Hangar-listing-0b1320?style=flat-square&labelColor=0b1320&color=dca13a" /></a>
  <a href="https://modrinth.com/plugin/mythicrod"><img alt="Modrinth" src="https://img.shields.io/badge/Modrinth-listing-0b1320?style=flat-square&labelColor=0b1320&color=00AF5C" /></a>
  <a href="https://crowdin.com/project/mythicrod"><img alt="Crowdin" src="https://img.shields.io/badge/Crowdin-translate-0b1320?style=flat-square&labelColor=0b1320&color=f0c75a" /></a>
  <a href="https://ko-fi.com/xcutiboo"><img alt="Ko-fi" src="https://img.shields.io/badge/Ko--fi-support-0b1320?style=flat-square&labelColor=0b1320&color=b87924" /></a>
</p>

![divider](assets/divider-feature.svg)

## Pick a path

| You are... | Start here |
| --- | --- |
| Running a server and want to install MythicRod | [Installation](installation.md) then [Quick start](commands.md) |
| Tuning drops, rods, or messages | [Configuration](configuration.md) and [Loot tables](loot-tables.md) |
| Debugging a live server | [Troubleshooting](troubleshooting.md), `/mythicrod status`, `/mythicrod validate` |
| Translating MythicRod | [Localization](localization.md), [Crowdin](localization/crowdin.md) |
| Building another plugin against the API | [Developer API](developer-api.md) |
| Tagging a public release | [Release guide](release.md) and the [Checklist](release/checklist.md) |

![divider](assets/divider.svg)

## Five-minute setup

1. Grab the latest `MythicRod-Paper-<version>.jar` from the [GitHub releases page](https://github.com/xcutiboo/MythicRod/releases).
2. Drop it in `plugins/` on a Paper 26.1.2 server. Folia is supported.
3. Start the server once. Default `config.yml`, `drops.yml`, and `lang/` files are written.
4. Customise `drops.yml`, then run `/mythicrod reload`.
5. Verify with `/mythicrod status`.

Full walkthrough: [Installation](installation.md).

![divider](assets/divider.svg)

## Version targets

| Item | Value |
| --- | --- |
| Plugin | `2026.1.0` (CalVer, year.release.patch) |
| API | Paper `26.1.2` (Minecraft `26.1.2`) |
| Java | 25+ |
| Optional integration | Nexo (`nexo:*` identifiers) |
| Bundled languages | `en_US`, `ja_JP` (rest sync from Crowdin) |
| Scheduler | Paper-first, Folia owner-thread handoffs verified |

![divider](assets/divider.svg)

## Status

- SonarCloud: 0 bugs / 0 vulnerabilities / 0 code smells / 0 hotspots.
- bStats: pluginId `31484` with 15 custom charts on top of the bStats defaults ([dashboard](https://bstats.org/plugin/bukkit/MythicRod/31484)).
- Crowdin: [crowdin.com/project/mythicrod](https://crowdin.com/project/mythicrod) (en_US source, ja_JP imported, 9 other targets open).
- Releases: [github.com/xcutiboo/MythicRod/releases](https://github.com/xcutiboo/MythicRod/releases).
- Folia 26.1.2 build 8: smoke test passed; runtime reports as `Folia` from `/mythicrod status`.
