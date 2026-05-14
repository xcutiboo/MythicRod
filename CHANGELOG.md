# Changelog

All notable changes to MythicRod are documented here.
Format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

---

## [Unreleased] — Production Readiness Pass

### Added

- **CodeQL workflow** — `.github/workflows/codeql.yml` runs CodeQL on push, PR, and a weekly Monday schedule for `java-kotlin` against the assembled jar. Surfaces results to GitHub's Security tab.
- **Dependency Review workflow** — `.github/workflows/dependency-review.yml` blocks PRs that introduce dependencies with high-severity vulnerabilities or `GPL-3.0` / `AGPL-3.0` licenses, and posts a summary comment on failure.
- **`CODEOWNERS`** — routes reviews to `@xcutiboo` by default, with stricter ownership on `mythicrod-api/`, `docs/developer-api.md`, `CHANGELOG.md`, and `.github/workflows/`.
- **Release artifact hardening** — `build.yml` release job now emits SHA-256 checksums for every published jar, attaches `*.jar.sha256` files to the GitHub release, and automatically marks tags containing `-rc`, `-beta`, `-alpha`, or `-snapshot` as pre-releases.
- **`/mythicrod validate`** — admin config health check (`mythicrod.admin.config`). Walks every loaded drop and reports unknown materials, invalid weights/amounts, Nexo identifiers when Nexo is not enabled, malformed or non-existent enchantments, unknown biome keys, permissions outside the `mythicrod.*` namespace, and duplicate identifiers within a category. Single-shot summary line at the end.
- **`/mythicrod testroll [biome] [count]`** — admin loot-tuning helper (`mythicrod.admin.debug`). Simulates up to 10,000 rolls in a given biome (defaults to the player's current biome) and prints a tier histogram plus the five most-frequent identifiers. Uses the live `DropManager` selector so output reflects the same weights players experience.
- **`/mythicrod rod inspect`** — admin diagnostic (`mythicrod.admin.debug`). Reads the held main-hand and off-hand items, reports whether each is a MythicRod fishing rod, the stored tier from PDC, and the configured rare-luck multiplier for that tier.
- **`LICENSE`** — MIT license file matching the badge in `README.md`.
- **`SECURITY.md`** — coordinated disclosure policy with private reporting channel and supported versions table.
- **`CONTRIBUTING.md`** — module layout, build/test commands, style conventions, and Folia-awareness notes for contributors.
- **`.editorconfig`** — consistent indentation and line-ending rules across Java, YAML, Markdown, and Gradle files.
- **Issue and PR templates** — structured bug-report and feature-request forms under `.github/ISSUE_TEMPLATE/` and a PR template that prompts for test plan and changelog updates.
- **Dependabot config** — weekly grouped updates for Gradle dependencies and GitHub Actions.
- **Test-report artifact upload on CI failure** — `build.yml` now uploads `build/reports/tests` and `build/test-results` when the build job fails, so PR reviewers can read failures without re-running the job.

### Removed

- **Stub `Disabled` workflows** — `.github/workflows/branch-protection.yml` and `.github/workflows/verify-release-workflow.yml` were empty placeholders cluttering the Actions tab; both deleted.
- **Tracked Eclipse `.factorypath` files** — `mythicrod-common/.factorypath` and `mythicrod-paper/.factorypath` were tracked despite being IDE state. Untracked and added `.factorypath` to `.gitignore`.

### Fixed

- **Drop weights and amounts now sanitize at load time** — `DropManager.createDrop` clamps weight to `>= 1`, amount to `1..64`, and `custom_model_data` to `>= 0`. A configured drop with `weight: -5` or `amount: 0` is now logged with the entry id and silently brought back into range instead of becoming a permanently-ineligible roll candidate. All drop construction paths (complex/mapped/simple/Nexo/defaults) route through `createDrop`, so the guarantee holds for every loaded drop.
- **`NexoItemProvider.createItem` no longer relies solely on `itemFromId` for missing-ID failures** — it now consults the cached `exists(String)` reflection method first and returns a dedicated `Nexo item not found: <id>` failure when the ID is unknown. Same change tightens the rejection of null/blank IDs at the entry point.
- **FQN sprawl removed** — six files (`NexoItemProvider`, `FishingListener`, `PaperItem`, `PaperPlatformItem`, `ItemBuilder`, `BrigadierCommandManager`) had inline `java.lang.reflect.Method`, `io.papermc.paper.datacomponent.DataComponentTypes`, `io.xcutiboo.mythicrod.api.Result`, and `org.bukkit.block.Biome` references in method bodies. All converted to proper imports.
- **`CustomDrop` mutable collections were not thread-safe** — `enchantments` (`HashMap`) and `itemFlags` (`ArrayList`) are mutated by admin GUI edits while fishing listeners read them on Folia entity/player region threads. Switched to `ConcurrentHashMap` and `CopyOnWriteArrayList` to remove the `ConcurrentModificationException` risk under concurrent read+write.
- **`StatisticsManager` locale-dependent `toLowerCase`** — `recordCatch` and `recordRodUse` lowercased without a `Locale`, which can produce surprising results under Turkish-locale JVMs. Switched both call sites to `toLowerCase(Locale.ROOT)`.
- **Placeholder rendering across all GUIs** — root cause: `Map.of("%key%", value)` was passing `%%key%%` to `LanguageManager.tr()`, which looks for `%key%`. All GUI menu files (`MainHubMenu`, `StatsMenu`, `ConfigMenu`, `DropsMenu`, `EditDropMenu`) now pass bare keys without `%` delimiters (`Map.of("count", "5")` instead of `Map.of("%count%", "5")`)
- **EditDropMenu context injection** — constructor changed from `(MythicRod, Player, CustomDrop, String)` to `(MythicRod, Player)` to satisfy `GUIManager.MenuFactory`. Drop and category are now read from the shared `context` map via `getContext("drop", CustomDrop.class)` and `getContext("category", String.class)` inside `build()`. Editable fields are initialised only once (first build, not refresh) to preserve in-flight edits
- **EditDropMenu registration** — `guiManager.registerMenu("editdrop", EditDropMenu::new)` was missing from `MythicRod.onEnable()`; added alongside the other six menu registrations
- **Drop item click-through to EditDropMenu** — `DropsMenu.buildCategoryView()` previously called `setItem(slot, dropItem)` with no click handler, making drop items unclickable. Each item now opens `editdrop` with `Map.of("drop", dropFinal, "category", selectedCategory)` for players holding `mythicrod.admin.config`
- **ja_JP.yml key structure** — the old file used completely wrong keys (`gui.main_title`, `gui_items.*`) that didn't match any call sites. Fully rewritten to mirror `en_US.yml`'s key namespace exactly (`gui.main.title`, `gui.stats.*`, `command.help.*`, `gui.edit_drop.*`, etc.) with accurate Japanese translations throughout
- **FishingListener catch messages** — previously used hardcoded English strings, bypassing locale and config templates, and later still treated configured drop display names as unparsed text so players could see literal MiniMessage tags like `<red><bold>` in catch chat. Catch messages now delegate to `ConfigManager` templates, preserve MiniMessage-formatted drop names via component placeholders, and accept legacy `&` formatting in both templates and configured item names
- **Language loading on update/reload** — bundled language defaults are now merged with disk overrides instead of choosing only one source, so newly added translation keys survive plugin updates without wiping customized lang files; bundled resources are also read explicitly as UTF-8
- **`/mythicrod stats <player>` lookup** — command resolution now falls back to MythicRod's persisted statistics store instead of relying only on Bukkit's offline-player cache, so stored stats remain queryable for offline uncached players
- **Stats read paths no longer fabricate state** — `/mythicrod stats` and the personal stats GUI now use read-only lookups instead of `getOrCreate()`, so viewing stats does not silently create zeroed player records
- **Stats command suggestions now match stored data** — player-name completion for `/mythicrod stats` includes persisted MythicRod statistics entries instead of only online players, aligning tab completion with actual lookup behavior
- **Delivered-catch accounting after disconnects** — if a reward is successfully dropped at the hook location and the player disconnects before the follow-up player-region callback, the catch is still recorded instead of being silently skipped
- **Reload semantics now include player preferences** — `players.yml` language preferences are reloaded from disk during plugin reload instead of remaining stale until full restart
- **Reload feedback is now truthful across admin entrypoints** — cache invalidation and reload success/failure ownership now live in `MythicRod.reload()`, so the command path and GUI path no longer diverge in behavior
- **GUI reload safety** — the main hub reload action now requires Shift+Click and its lore reflects the real reload scope (`config.yml`, `drops.yml`, `players.yml`, and language files)
- **Uncommon rarity feedback mismatch** — uncommon drops already had their own message and XP tier, but particle feedback still collapsed them into the common branch; `FishingListener` now gives uncommon catches a distinct intermediate particle burst
- **Player preference persistence storm / reload race** — language changes previously queued one full `players.yml` write per click and reload could repopulate from disk before pending async writes landed; `PlayerPreferences` now coalesces save requests and flushes pending changes before reload or shutdown
- **Player preference snapshot race during reload** — `PlayerPreferences.reloadFromDisk()` previously cleared and repopulated its live map in place, so concurrent reads could briefly fall back to the server default language while reload was rebuilding state; preferences now reload through an atomic snapshot swap, and the common-module regression suite proves the old value stays visible until the new snapshot is ready
- **Catch template fallback drift** — `ConfigManager`'s blank/reset fallbacks had regressed to header-only catch strings without `{amount}` and `{item}`, and the shipped `config.yml` did not expose the `messages.catch.*` keys at all. Catch-template defaults are now canonicalized in code, blank values restore the full message, and fresh configs ship the full `messages.catch.*` section
- **Configured item name/lore legacy formatting drift** — catch chat now tolerates legacy `&` formatting in configured drop names, and `ItemBuilder` now does the same for actual reward item names and lore so chat output and item metadata stay consistent for migrated configs
- **Reload autosave failure mode** — a reload exception after `cancelStatisticsSaveTask()` could leave statistics autosave permanently disabled until restart; `MythicRod.reload()` now restores the autosave task in a `finally` guard when reload aborts mid-flight
- **Statistics autosave write amplification / failure recovery** — `StatisticsManager.saveAll()` previously rewrote the full `statistics.yml` once per dirty player and dirty stats could still be dropped on failed eviction/unload writes; it now batches dirty-entry updates in memory, saves the file once per autosave/reload cycle, and keeps dirty stats tracked when a flush fails
- **Removed dead fishing/runtime scaffolding** — the unreferenced `FishingService`, `FishingMinigameService`, and `PlatformPlayerRegistry` classes were deleted after a full workspace reference audit confirmed they were not wired into MythicRod’s runtime at all
- **GUI drop-save vs reload race** — async `EditDropMenu` save/delete operations could still be in flight when reload read `drops.yml`, letting stale pre-reload writes land afterward; `DropManager` now tracks pending async persistence work and `MythicRod.reload()` waits before reading or publishing drop data
- **Stale GUI sessions across reloads** — open MythicRod menus, especially stateful admin screens like `ConfigMenu` and `EditDropMenu`, could outlive reload and keep acting on pre-reload draft state; reload is now single-flight, GUI sessions are invalidated on reload, stale menus self-close on the next interaction, and new menus are blocked while reload is in progress
- **Dead runtime scaffolding removed** — unused display-entity effects, unused pagination base class, unused auto-retrieve player data, stale GUI language aliases, and unused soft-dependencies were removed after reference audits confirmed they were not wired into runtime behavior
- **Public stats snapshot invariants** — `PlayerStatSnapshot` now rejects every negative counter and null required value, not only a subset of counters
- **Drops command usage text on upgraded installs** — the category hint moved to `drops.usage-hint` so old copied language files with the stale `%category%` text cannot override the corrected bundled message
- **Java 25 documentation lint** — a dangling Javadoc block in `DropSelector` is now attached to the weighted-selection implementation instead of failing Java 25 `-Werror` builds
- **Stats breakdown honesty** — the stats GUI and command output now show a rarity-tier breakdown instead of claiming to show top materials; the old GUI path tried to resolve tier names as Bukkit materials and silently skipped every row
- **Dead hook-cleanup config path** — `hook-cleanup-interval-seconds` is no longer read, clamped, saved, or shipped in the default config now that hook cleanup is automatic
- **Duplicate drop edit safety** — GUI save/delete now targets the exact drop row the admin opened instead of matching by identifier, so duplicate materials in the same category no longer update the wrong row or delete every matching row
- **XP particle setting now does real work** — `features.particles.xp-particle` is used by fishing XP feedback instead of being a dead config value
- **Legacy hex color migration** — configured strings using `&#RRGGBB` now bypass the legacy serializer path and migrate directly to MiniMessage hex tags, preventing serializer loss for migrated configs
- **Removed stale config-menu clutter** — the old informational hook-cleanup tile was removed from `ConfigMenu` and bundled language files now that the runtime setting no longer exists
- **Drop editor name preset crash** — server logs from GUI testing showed repeated `NullPointerException` failures when opening `EditDropMenu` because `List.of(null, ...)` rejected the blank-name preset. The editor now builds that preset list safely.
- **Unreadable black GUI titles on upgraded installs** — bundled GUI titles now use MythicRod's gold/aqua/yellow/green palette instead of `<black>`, and language loading ignores known old shipped black title defaults from disk while preserving real custom overrides.
- **Noisy menu transition feedback** — opening one MythicRod menu from another now suppresses the old menu's close sound so navigation feels like a transition instead of a close/open stutter.
- **Rod selection no-op feedback** — selected rod tiers now glow only when active, and clicking the already-selected tier tells the player it is already selected instead of silently doing nothing.
- **Drops command GUI parity** — players running `/mythicrod drops <category>` now open that category in the drops GUI instead of receiving console-style text output; console senders keep the text view.
- **Confusing main hub layout** — the old prismarine/cyan decorative row was removed, top-level actions are now centered around Drops, Rod & Effects, and Statistics, and admin actions sit in a separate row.
- **Config toggle localization drift** — shared toggle items no longer hardcode English `ENABLED/DISABLED` lore; status/action lines now come from the active language file.
- **Drop category command friction** — `/mythicrod drops ocean` now resolves to `biome_ocean`, command suggestions include short biome aliases, and unknown categories now show a concrete category-picking hint.
- **Public event invalid-input handling** — `MythicRodFishCatchEvent` now validates null/AIR reward items through a deliberate boundary check, and `MythicRodRewardRollEvent` validates required constructor fields.
- **README drift** — the configuration and Developer API examples now match the current nested config keys and `PlayerStatSnapshot`/`StatType` method names.
- **Stale language overrides after upgrades** — known old drops command strings and black legacy menu-alias titles now refresh on disk the same way active GUI titles do, so copied `plugins/MythicRod/lang/*.yml` files no longer keep ugly or misleading defaults forever.
- **Duplicate biome drop pools after GUI saves** — upgraded configs that contain both old `biome-drops.*` sections and modern `drops.biome_*` categories no longer merge both copies. Modern `drops.*` categories win, legacy-only biome sections still load, and future GUI saves remove stale legacy drop sections.
- **Drop eligibility toggles now affect runtime rewards** — `features.permissions.enabled` and `features.drops.biome-specific.enabled` now flow into the weighted selector instead of only changing GUI/config state. Permission-gated drops are excluded when the player lacks the required node, biome-constrained drops are excluded when biome rewards are disabled, and constrained biome drops no longer become globally eligible when biome context is missing.
- **Fresh default drops no longer ship stale legacy biome sections** — bundled `drops.yml` now uses modern `drops.biome_*` categories. Legacy `biome-drops:*` still loads for upgraded installs, but new servers no longer get a cleanup warning immediately after installation.
- **Safer default reward gates** — permissions are enabled by default, and the `global`, `rare`, and `legendary` categories receive implicit category permissions unless a drop defines a custom permission. The bundled biome defaults also avoid spawn-egg rewards that would be too easy to abuse on survival servers.
- **Config schema upgrade persistence** — saving config through MythicRod now writes the current `config-version`, so upgraded servers do not keep reporting an old schema after an admin saves settings.
- **Dead cache facade removed** — `MythicRodCache` was initialized and shown by `/mythicrod debug`, but nothing populated it. The debug command now reports real runtime counts for configured drops, categories, tracked players, and catches since reload.
- **Rod tiers now affect reward rolls** — Basic, Advanced, and Legendary rod tiers now apply configurable rare-drop luck multipliers during fishing. Held MythicRod items take priority, GUI-selected tiers still work, and permission checks prevent stale or spoofed high-tier selections from applying.
- **Dead rod-upgrade event removed** — `MythicRodRodUpgradeEvent` described an upgrade system that MythicRod does not implement or fire. The public Paper event surface now sticks to the real fishing catch and reward-roll extension points.
- **Vanilla caught-item flash on Paper** — ordinary Paper servers now handle custom reward delivery immediately in the `CAUGHT_FISH` event path instead of routing through next-tick owner schedulers, preventing a brief client-side glimpse of the vanilla caught item before inventory or player-drop delivery. Folia still uses owner-aware scheduler handoffs.
- **Drop weight wording** — command output and the edit-drop GUI no longer render configured weights as percentages. They now label them as weights so admins understand MythicRod uses relative weighted rolls.
- **Copied language placeholder refresh** — upgraded language files that already said "weight" but still used the old `%chance%` placeholder are refreshed to `%weight%` before command output can leak raw tokens.
- **Statistic last-fished persistence** — `PlayerStats.lastFished` is now written to and restored from `statistics.yml`, so API snapshots, commands, and stats GUIs do not lose the player's most recent fishing timestamp after restart or cache reload.
- **GUI inventory collect safety** — MythicRod menus now cancel bottom-inventory collect/hotbar-transfer actions that can reach into the top inventory, closing a common inventory-GUI exploit path while still allowing ordinary player-inventory clicks.
- **Category label polish** — category formatting now title-cases multi-word identifiers such as `biome_mushroom_fields` as `Mushroom Fields Biome` instead of leaking underscores into command and GUI labels.
- **Drop editor customization gap** — the edit GUI no longer limits admins to preset names and preset lore. It now captures Paper chat input for item identifiers, custom names, and custom lore lines, then returns to the editor on the player scheduler.
- **Drop item identifier persistence** — GUI saves can now change the selected drop's `identifier`/`nexo-item` value instead of preserving the original material forever.
- **Drop editor full-field persistence** — the edit GUI now exposes and saves custom model data, permission gates, biome filters, enchantments, and item flags instead of silently preserving those YAML fields with no in-game control. Weight and amount controls also support exact chat input, category pages can create a new drop row from an item id, and old copied language files refresh the stale `+1/-1` control text.
- **Config command parity** — `/mythicrod config` now exposes the core admin settings from the GUI for command-first workflows: sounds, particles, statistics, biome drops, drop permissions, debug logging, reward delivery mode, and statistics save interval.
- **Config schema cleanup** — `features.drops.delivery-mode` is now the only reward-delivery setting. The stale boolean delivery alias was removed from code, defaults, and tests, and the bundled config schema is now version 8.
- **Drop API cleanup** — `PlatformDrop` now exposes `getWeight()` as the single public weight accessor. Drop loading still reads the previous `chance` key for disk migration, but reports it once per load and saves future GUI edits as `weight`.
- **Drops menu custom-item display** — Nexo drops and namespaced materials now get clearer display labels and fallback icons in the drop browser instead of looking like plain Paper items.
- **Removed stale item builder path** — the unused `paper.util.ItemStackFactory` helper was deleted after reference auditing confirmed that runtime rewards are built by `FishingListener` through the platform item factory.
- **Paper dependency metadata for Nexo** — `paper-plugin.yml` now declares optional Nexo support under `dependencies.server` with `load: BEFORE` and `join-classpath: true` instead of Bukkit-style `softdepend`, matching Paper plugin dependency semantics.
- **Nexo enabled checks** — Paper platform reporting now uses `PluginManager#isPluginEnabled("Nexo")` so metrics/API state does not report a disabled plugin as active.
- **Malformed configured text safety** — GUI titles, GUI messages, command feedback, platform broadcasts, and item previews now parse through a shared lenient MiniMessage path. Bad admin-entered MiniMessage falls back to plain text instead of breaking menu opens or command output.
- **Edit-drop save status key** — the save button no longer uses YAML keys named `yes`/`no`, which YAML parsers can treat as booleans. It now uses explicit `status_yes`/`status_no` keys so the glow summary renders reliably.
- **Bundled language dead keys** — removed unused fishing, leaderboard, config reload, rod upgrade/info, and logging language sections from `en_US.yml` and `ja_JP.yml`; the bundled language files now match runtime call sites.
- **Unknown language override drift** — disk language overrides for keys that no longer exist in the bundled language defaults are ignored at load time, preventing stale copied keys from reappearing in the runtime translation map.
- **Duplicate console prefix** — startup/status lines no longer add a second `[MythicRod]` prefix on top of Paper's plugin logger prefix.
- **Command help discoverability** — `/mythicrod help` now lists the help command itself along with permission-filtered subcommands.
- **Drop editor delete race from runtime testing** — repeated Shift+Click delete actions could submit multiple async writes for the same selected row, producing `Selected drop is no longer present` stack traces after the first delete succeeded. Save/delete actions now enter a visible in-progress state, repeated clicks get a player-facing wait message, stale concurrent edits are reported without stack traces, and `drops.yml` writes are serialized.
- **Particle config safety** — particle names are now validated against the active Paper `Particle` enum during startup/reload and command entry. Data-bound particles such as dust, block, item, spell, vibration, and trail receive safe default data instead of failing at spawn time.
- **bStats startup isolation** — metrics initialization and custom-chart registration are wrapped so a bStats runtime issue cannot prevent MythicRod from enabling.
- **Reward delivery hardening** — external providers and catch-event listeners can no longer deliver oversized reward stacks; final items are cloned and clamped to the material max stack size. Rewards dropped at the player now set owner/thrower metadata and immediate pickup delay.
- **Build metadata drift** — Gradle now uses the `version` in `gradle.properties`, CI builds on Java 25, and the artifact upload no longer references a removed Spigot module.
- **Public API documentation style** — the stable `mythicrod-api` surface plus Paper service/event entry points now use Java Markdown Javadocs (`///`) with clearer lifecycle, threading, and integration guidance.

### Added

- **`gui.edit_drop.*` key set** in both `en_US.yml` and `ja_JP.yml` covering all 40+ strings used by `EditDropMenu` (title, weight/amount/lore/glow/save/delete/reset/back/info panels)
- **Delete confirmation** in `EditDropMenu` — requires Shift+Click; any other click shows a warning message and plays error sound
- **Lore editor** now removes the last line on Right-Click, accepts custom typed lines on Left-Click, supports Shift+Left replacement, and keeps the max at 10 lines
- **Reusable GUI chat input session** — `GUIManager` now owns a short-lived, reload-aware chat input flow for inventory editors. It cancels captured chat, times out stale sessions, clears pending input on quit/shutdown/reload, and hands callbacks back to the correct player scheduler.
- **Shared configured-text parser** — `ConfiguredText` centralizes lenient MiniMessage parsing for config, language, command, and GUI editor text.
- **Focused JUnit regression coverage for `PlayerPreferences`** — the common module now has a real test task and a dedicated persistence harness that verifies reload waits for an in-flight save, shutdown waits for an in-flight save, and disk reload picks up the latest persisted snapshot
- **Focused JUnit regression coverage for `DropManager` reload coordination** — the common test suite now proves that `DropManager.reload(...)` does not publish a new drop table while async persistence is still marked in flight, protecting the reload ordering fix against future regressions
- **Focused JUnit regression coverage for drop eligibility policy** — common tests now prove permission-gated categories, implicit `biome_*` conditions, missing biome context, and disabled biome-specific rewards behave as configured.
- **Hangar publishing setup** — the Paper module now uses the official Hangar Gradle plugin with the shaded jar, README resource page sync, CHANGELOG release text, `paperVersion` compatibility metadata, and a token-gated GitHub Actions workflow.
- **Particle option regression coverage** — tests now assert that every current Paper particle has a MythicRod-safe configured-data path and that command suggestions remain sorted.
- **Focused JUnit regression coverage for catch message formatting** — `mythicrod-paper` now has a targeted test proving MiniMessage-formatted and legacy-formatted drop names render as styled components instead of leaking raw tags into catch chat
- **Focused JUnit regression coverage for item display text migration** — `ItemBuilderFormattingTest` now locks in legacy `&` code migration for configured item names and lore before MiniMessage deserialization
- **Focused JUnit regression coverage for malformed configured text** — `ItemBuilderFormattingTest` now proves invalid MiniMessage falls back to a plain visible component instead of throwing.
- **Focused JUnit regression coverage for catch template defaults** — `ConfigManagerTest` now locks in that missing or blank `messages.catch.*` values still fall back to the full catch template with `{amount}` and `{item}` placeholders intact
- **Focused JUnit regression coverage for tier statistics** — `PlayerStatsTest` locks in the immutable rarity-tier breakdown used by command and GUI stats views
- **Focused JUnit regression coverage for duplicate drop edits** — `DropManagerTest` proves exact-row GUI updates and deletes remain safe when identifiers repeat
- **Focused JUnit regression coverage for drop identifier edits** — `DropManagerTest` proves GUI updates can change a duplicate-row drop from one vanilla item to another and can persist Nexo identifiers.
- **Focused JUnit regression coverage for particle controls** — `ParticleOptionsTest` proves the curated GUI/command particle suggestions are valid Paper particle names
- **Focused JUnit regression coverage for legacy text migration** — `MiniMessageMigratorTest` locks in ampersand legacy-code detection and `&#RRGGBB` migration behavior
- **Full bStats chart set** — metrics now include Paper/Minecraft version, Folia runtime status, language, profile, reward delivery mode, feature toggles, Nexo availability, configured drop/category counts, tracked player count, and total custom catches
- **Regression coverage for language override migration** — the Paper test suite now proves old shipped black GUI title defaults refresh while real custom language titles are preserved.
- **Regression coverage for upgraded drop configs and category labels** — tests now lock in modern-vs-legacy biome drop precedence and human-readable multi-word category formatting.
- **Regression coverage for statistics timestamps** — `StatisticsManagerTest` now proves `last_fished` survives save and reload.

### Changed

- `EditDropMenu.buildInfoPanel()` now calls `tr("gui.edit_drop.info.lore1", Map.of("identifier", ...))` and `lore2` with material name placeholders — previously used hardcoded English strings
- `DropsMenu.buildCategoryView()` — drop items now have a click handler instead of being static display items
- `MythicRod.java` import list — `EditDropMenu` import added; menu registrations aligned with consistent spacing
- `mythicrod-common/src/main/resources/config.yml` now documents `messages.catch.*` as a first-class configurable feature with the same default templates `ConfigManager` uses at runtime
- Stats leaderboard/menu copy now describes persisted recorded totals instead of session-only/in-memory behavior
- Public API extracted into `mythicrod-api`; `MythicRodAPI` now exposes read-only drop inspection through `DropCatalog` instead of leaking mutable internal drop services
- Internal runtime bridge renamed to `MythicRodRuntime`, and Paper-only GUI/item code now lives under `io.xcutiboo.mythicrod.paper.*` so module ownership is explicit in package names
- README rewritten with full installation guide, drop field reference table, Developer API examples (events, stats, external providers), permissions table, language guide, and build instructions
- Project target migrated to Java `25` and Paper API `26.1.2.build.64-stable` after official Paper metadata confirmed that generation as the latest stable line
- Gradle wrapper upgraded to `9.5.1`; Shadow, run-paper, bStats, Caffeine, Lombok, JUnit, and JetBrains annotations were refreshed from official metadata
- The Paper shaded jar no longer bundles unused HikariCP, Lettuce, or Configurate dependencies
- Particle settings are now GUI-first: the config menu can cycle catch, bubble, success, and XP particles directly; `/mythicrod particle xp <type>` was added for command parity
- The drops GUI now paginates category and drop views instead of silently truncating entries past the visible content area
- Player language preference saves still serialize through one worker, but the worker now uses a named Java 25 virtual thread instead of a raw daemon platform thread
- GUI inventory titles now share a concise branded pattern: `MythicRod • Hub`, `MythicRod • Drops`, `MythicRod • Config`, `MythicRod • Stats`, and equivalent Japanese titles.
- Drops and console category listings now use deterministic ordering: global, rare, legendary, then biome categories by display name.
- Config menu controls now use distinct materials for sounds, particles, statistics, and biome drops, and the stats autosave interval draft is clamped to the same 60–3600 second range documented in the GUI.
- The internal `PrettyLogger` helper was reduced to the startup/status behavior MythicRod actually uses.
- Critical runtime classes now use ordinary imports and context-first log messages instead of fully qualified type clutter and duplicate logger prefixes.

---

## [1.1.0] — Production Audit Cycles 1–6

### Added

- **Custom Bukkit events** — `MythicRodFishCatchEvent` (cancellable, carries `rewardItem`) and `MythicRodRewardRollEvent` (luck multiplier + force-drop override); both with correct `HandlerList` boilerplate
- **`ExternalDropProvider`** — platform-agnostic interface for third-party drop registration keyed by namespaced strings
- **`PlayerStatSnapshot`** — immutable Java record with factory `empty(UUID, String)` and nested `StatType` enum; compact constructor validates non-negative counts
- **`DropRegistry`** — `ConcurrentHashMap`-backed registry; supports in-place reload (`clear()` + repopulate) without breaking existing API references
- **`MythicRodAPI` abstract surface** — `getVersion()`, `getDropRegistry()`, `registerExternalDropProvider()`, `unregisterExternalDropProvider()`, `getExternalDropProvider()`, `getExternalDropProviders()`, `getPlayerStats(UUID)`, `getTopPlayers(StatType, int)`, `flushAllStats()`
- **`PaperMythicRodAPI`** — full implementation; `ConcurrentHashMap` external-provider registry; `toSnapshot(PlayerStats)` maps all tier counters; `flushAllStats()` runs `CompletableFuture.runAsync`
- **`ServicesManager` registration** — `MythicRodAPI` registered via `ServicePriority.Normal` on enable; unregistered on disable
- **`StatisticsManager`** — Caffeine TTL cache (`expireAfterAccess(30 min)`), `AtomicLong` total-catch counter, `ConcurrentHashMap` dirty-set, `recordCatch(UUID, String)` by tier string, `getTopFishers(int)` returns `List<PlayerStats>` sorted descending
- **`EditDropMenu`** — in-game drop editor GUI (weight/amount/name/lore/glow; save/reset/delete)
- **`RodMenu`** — fishing rod management GUI
- **`mythicrod-paper/src/main/resources/lang/en_US.yml`** — complete key set for all 7 menus plus command output, fishing messages, leaderboard, and logging; MiniMessage throughout

### Changed

- **`FishingListener`** — fires `MythicRodRewardRollEvent` before drop selection (honours `forceDrop`/`luckMultiplier`); fires `MythicRodFishCatchEvent` on player's EntityScheduler before item dispensing (cancellable); `recordCatch` now uses `(UUID, category)` signature
- **`DropSelector` hot path** — replaced `TreeMap<Integer, CustomDrop>` cumulative-weight builder with a primitive `int[]` binary search: eliminates `Integer` boxing, `TreeMap.Entry` allocations, and pointer-chasing cache misses
- **`DisplayEffectsService`** — non-atomic `taskId` → `AtomicInteger`; animation tasks cancelled before entity removal; null-checked all `EntityScheduler` return values; replaced infinite float-up with bounded sine-wave bob oscillation
- **`StatsMenu` / `BrigadierCommandManager`** — migrated from `StatisticsManager.PlayerStats` inner class to `io.xcutiboo.mythicrod.stats.PlayerStats`
- **`paper-plugin.yml`** — full hierarchical permission tree with descriptions, defaults, and parent-child relationships
- **Multi-module layout** — `mythicrod-common` (platform-agnostic core) + `mythicrod-paper` (Paper/Folia implementation)

### Fixed

- `bStats SingleLineChart` cast: `statisticsManager.getTotalCatches()` returns `long` — wrapped with `(int) Math.min(value, Integer.MAX_VALUE)`
- `PaperMythicRodAPI` constructor call updated from old `(PlatformServer, PlatformScheduler)` to `(version, dropManager, dropRegistry, statisticsManager)`
- `PlayerStats` exposes the current tier breakdown as an immutable `Map<String, Integer>`
- `BrigadierCommandManager.sendHelpMessage()` — migrated from hardcoded strings to `languageManager.tr("command.help.*")`

---

## [1.0.2] — 2025-12-16

### Added

- Multi-module project structure (common, paper)
- Paper: Native Brigadier command system

### Changed

- Split into platform-specific implementations
- Paper uses native Adventure API

### Fixed

- Hook state edge case where waiting after bite could bypass custom drops

---

## [1.0.1] — 2025-12-14

### Fixed

- Hook state cleanup preventing custom drops after delayed catch

---

## [1.0.0] — 2025-12-13

### Added

- Custom fishing drop system with weighted categories
- Biome-specific drops
- Permission-based drop categories
- Statistics tracking and leaderboards
- Item customization (names, lore, enchantments, glow)
- In-game GUI menus
- Multi-language support (English, Japanese)
- Tiered sound and particle effects on catch
- Developer API
