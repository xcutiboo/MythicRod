# Changelog

All notable changes to MythicRod are documented here.
Format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

---

## [Unreleased] — Production Readiness Pass

### Fixed
- **Placeholder rendering across all GUIs** — root cause: `Map.of("%key%", value)` was passing `%%key%%` to `LanguageManager.tr()`, which looks for `%key%`. All GUI menu files (`MainHubMenu`, `StatsMenu`, `ConfigMenu`, `DropsMenu`, `EditDropMenu`) now pass bare keys without `%` delimiters (`Map.of("count", "5")` instead of `Map.of("%count%", "5")`)
- **EditDropMenu context injection** — constructor changed from `(MythicRod, Player, CustomDrop, String)` to `(MythicRod, Player)` to satisfy `GUIManager.MenuFactory`. Drop and category are now read from the shared `context` map via `getContext("drop", CustomDrop.class)` and `getContext("category", String.class)` inside `build()`. Editable fields are initialised only once (first build, not refresh) to preserve in-flight edits
- **EditDropMenu registration** — `guiManager.registerMenu("editdrop", EditDropMenu::new)` was missing from `MythicRod.onEnable()`; added alongside the other six menu registrations
- **Drop item click-through to EditDropMenu** — `DropsMenu.buildCategoryView()` previously called `setItem(slot, dropItem)` with no click handler, making drop items unclickable. Each item now opens `editdrop` with `Map.of("drop", dropFinal, "category", selectedCategory)` for players holding `mythicrod.admin.config`
- **ja_JP.yml key structure** — the old file used completely wrong keys (`gui.main_title`, `gui_items.*`) that didn't match any call sites. Fully rewritten to mirror `en_US.yml`'s key namespace exactly (`gui.main.title`, `gui.stats.*`, `command.help.*`, `gui.edit_drop.*`, etc.) with accurate Japanese translations throughout
- **FishingListener catch messages** — previously used hardcoded English strings, bypassing locale and config templates. Now delegates to `ConfigManager.getMsgCommon/Rare/Legendary()` with `Placeholder.unparsed()` for injection-safe item name substitution

### Added
- **`gui.edit_drop.*` key set** in both `en_US.yml` and `ja_JP.yml` covering all 40+ strings used by `EditDropMenu` (title, chance/amount/lore/glow/save/delete/reset/back/info panels)
- **Delete confirmation** in `EditDropMenu` — requires Shift+Click; any other click shows a warning message and plays error sound
- **Lore editor** now removes the last line on Right-Click (instead of clearing all); Left-Click adds a preset line from a rotating pool; max raised to 10 lines

### Changed
- `EditDropMenu.buildInfoPanel()` now calls `tr("gui.edit_drop.info.lore1", Map.of("identifier", ...))` and `lore2` with material name placeholders — previously used hardcoded English strings
- `DropsMenu.buildCategoryView()` — drop items now have a click handler instead of being static display items
- `MythicRod.java` import list — `EditDropMenu` import added; menu registrations aligned with consistent spacing
- README rewritten with full installation guide, drop field reference table, Developer API examples (events, stats, external providers), permissions table, language guide, and build instructions

---

## [1.1.0] — Production Audit Cycles 1–6

### Added
- **Custom Bukkit events** — `MythicRodFishCatchEvent` (cancellable, carries `rewardItem`), `MythicRodRewardRollEvent` (luck multiplier + force-drop override), `MythicRodRodUpgradeEvent` (cancellable, `UpgradeType` enum); all with correct `HandlerList` boilerplate
- **`ExternalDropProvider`** — platform-agnostic interface for third-party drop registration keyed by namespaced strings
- **`PlayerStatSnapshot`** — Java 21 record (immutable) with factory `empty(UUID, String)` and nested `StatType` enum; compact constructor validates non-negative counts
- **`DropRegistry`** — `ConcurrentHashMap`-backed registry; supports in-place reload (`clear()` + repopulate) without breaking existing API references
- **`MythicRodAPI` abstract surface** — `getVersion()`, `getDropRegistry()`, `registerExternalDropProvider()`, `unregisterExternalDropProvider()`, `getExternalDropProvider()`, `getExternalDropProviders()`, `getPlayerStats(UUID)`, `getTopPlayers(StatType, int)`, `flushAllStats()`
- **`PaperMythicRodAPI`** — full implementation; `ConcurrentHashMap` external-provider registry; `toSnapshot(PlayerStats)` maps all tier counters; `flushAllStats()` runs `CompletableFuture.runAsync`
- **`ServicesManager` registration** — `MythicRodAPI` registered via `ServicePriority.Normal` on enable; unregistered on disable
- **`StatisticsManager`** — Caffeine TTL cache (`expireAfterAccess(30 min)`), `AtomicLong` total-catch counter, `ConcurrentHashMap` dirty-set, `recordCatch(UUID, String)` by tier string, `getTopFishers(int)` returns `List<PlayerStats>` sorted descending
- **`EditDropMenu`** — in-game drop editor GUI (chance/amount/name/lore/glow; save/reset/delete)
- **`RodMenu`** — fishing rod management GUI stub
- **`lang/en_US.yml`** — complete key set for all 7 menus plus command output, fishing messages, leaderboard, and logging; MiniMessage throughout

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
- `getMaterialCounts()` backward-compatibility stub added to `PlayerStats` returning tier breakdown as immutable `Map<String, Integer>`
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
