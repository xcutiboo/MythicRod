# Changelog

## [Unreleased] - Production Audit Cycles 1–6

### Added
- **Custom Bukkit Events** — `MythicRodFishCatchEvent` (cancellable, carries rewardItem), `MythicRodRewardRollEvent` (luck multiplier + force-drop override), `MythicRodRodUpgradeEvent` (cancellable, `UpgradeType` enum), all with proper `HandlerList` boilerplate
- **ExternalDropProvider** — platform-agnostic interface for third-party drop registration via `NamespacedKey`-style string keys
- **PlayerStatSnapshot** — Java 21 record (immutable) with factory `empty(UUID, String)` and nested `StatType` enum; compact constructor validates non-negative counts
- **DropRegistry** — `ConcurrentHashMap`-backed registry; supports in-place reload (`clear()` + repopulate) without breaking existing API references
- **MythicRodAPI** — new abstract methods: `getVersion()`, `getDropRegistry()`, `registerExternalDropProvider()`, `unregisterExternalDropProvider()`, `getExternalDropProvider()`, `getExternalDropProviders()`, `getPlayerStats(UUID)`, `getTopPlayers(StatType, int)`, `flushAllStats()`
- **PaperMythicRodAPI** — full implementation with `ConcurrentHashMap` external-provider registry; `toSnapshot(PlayerStats)` maps all tier counters; `flushAllStats()` runs `CompletableFuture.runAsync`
- **ServicesManager registration** — `MythicRodAPI` registered via `ServicePriority.Normal` on enable; unregistered on disable
- **StatisticsManager** — Caffeine TTL cache (`expireAfterAccess(30 min)`), `AtomicLong` total-catch counter, `ConcurrentHashMap` dirty-set, `recordCatch(UUID, String)` by tier string, `getTopFishers(int)` returns `List<PlayerStats>` sorted descending

### Changed
- **FishingListener** — fires `MythicRodRewardRollEvent` before drop selection (honours `forceDrop`/`luckMultiplier`); fires `MythicRodFishCatchEvent` on player's EntityScheduler before item dispensing (cancellable); `recordCatch` now uses `(UUID, category)` signature
- **DropSelector hot path** — replaced `TreeMap<Integer, CustomDrop>` cumulative-weight builder with a primitive `int[]` binary search: eliminates `Integer` boxing, `TreeMap.Entry` allocations, and pointer-chasing cache misses per fish-catch event
- **DisplayEffectsService** — non-atomic `taskId` → `AtomicInteger`; animation tasks cancelled before entity removal to prevent retired-callback races; null-checked all `EntityScheduler` return values; replaced infinite float-up with bounded sine-wave bob oscillation
- **StatsMenu / BrigadierCommandManager** — migrated from `StatisticsManager.PlayerStats` inner class to `io.xcutiboo.mythicrod.stats.PlayerStats`; `getOrCreate(UUID)`, `getTotalCaught()`, `getRareCaught()`, `List<PlayerStats>` leaderboard
- **lang/en_US.yml** — added all missing `gui.main.*`, `gui.config.*` keys that the menu classes actually call via `tr()`; removed key naming mismatch between `gui.main_hub.*` and `gui.main.*`
- **paper-plugin.yml** — full hierarchical permission tree with descriptions, defaults, and parent-child relationships

### Fixed
- `bStats SingleLineChart` cast: `statisticsManager.getTotalCatches()` returns `long` — wrapped with `(int) Math.min(value, Integer.MAX_VALUE)`
- `MythicRod.java` bogus `@Override` on `getPlatformServer()` / `getPlatformScheduler()` — removed (not declared in any supertype)
- `PaperMythicRodAPI` constructor call updated from old `(PlatformServer, PlatformScheduler)` to `(version, dropManager, dropRegistry, statisticsManager)`
- `getMaterialCounts()` backward-compatibility stub added to `PlayerStats` returning tier breakdown as immutable `Map<String, Integer>`

## [1.0.2] - 2025-12-16

### Added

- Multi-module project structure (common, paper, spigot)
- Paper: Native Brigadier command system
- Spigot: Full feature parity with Paper

### Changed

- Split into platform-specific implementations
- Paper uses native Adventure API
- Spigot bundles Adventure Platform (relocated to prevent conflicts)

### Fixed

- Hook state edge case where waiting after bite could bypass custom drops

## [1.0.1] - 2025-12-14

### Fixed

- Hook state cleanup preventing custom drops after delayed catch

## [1.0.0] - 2025-12-13

### Added

- Custom fishing drop system
- Biome-specific drops
- Permission-based categories
- Statistics tracking and leaderboards
- Item customization (names, lore, enchantments, glow)
- GUI menus
- Multi-language support (English, Japanese)
- Sound and particle effects
- Developer API
