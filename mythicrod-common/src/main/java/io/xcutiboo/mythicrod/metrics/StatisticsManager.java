package io.xcutiboo.mythicrod.metrics;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.xcutiboo.mythicrod.MythicRodPlugin;
import io.xcutiboo.mythicrod.api.platform.PlatformConfiguration;
import io.xcutiboo.mythicrod.stats.PlayerStats;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages in-memory player fishing statistics with Caffeine TTL-bounded caching.
 *
 * <h2>Thread Safety</h2>
 * <ul>
 *   <li>The active-players cache uses Caffeine — fully thread-safe.</li>
 *   <li>The dirty set uses {@link ConcurrentHashMap} as a set.</li>
 *   <li>{@link #saveAll()} is intended to be called from the async scheduler.</li>
 *   <li>{@link #getStats(UUID)} and {@link #recordCatch} may be called from
 *       any entity region thread.</li>
 * </ul>
 *
 * <h2>Cache Design</h2>
 * Stats for online players are kept indefinitely while they are active.
 * After login/access, entries expire from the cache after
 * {@value #EXPIRE_AFTER_ACCESS_MINUTES} minutes of no access.
 * This prevents {@code OutOfMemoryError} from unbounded accumulation.
 */
public final class StatisticsManager {

    private static final int EXPIRE_AFTER_ACCESS_MINUTES = 30;
    private static final int INITIAL_CAPACITY = 64;

    private final MythicRodPlugin plugin;
    private final Logger logger;

    /**
     * TTL-bounded, thread-safe Caffeine cache.
     * Evicts entries not accessed for {@value #EXPIRE_AFTER_ACCESS_MINUTES} minutes.
     */
    private final Cache<UUID, PlayerStats> statsCache;

    /**
     * Tracks which UUIDs have unsaved mutations.
     * Used as a set: value is always {@link Boolean#TRUE}.
     */
    private final ConcurrentHashMap<UUID, Boolean> dirtySet = new ConcurrentHashMap<>();

    /** Total catches across all players since last reload — for bStats. */
    private final java.util.concurrent.atomic.AtomicLong totalCatchesGlobal =
            new java.util.concurrent.atomic.AtomicLong(0L);

    public StatisticsManager(@NotNull MythicRodPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.statsCache = Caffeine.<UUID, PlayerStats>newBuilder()
                .initialCapacity(INITIAL_CAPACITY)
                .expireAfterAccess(Duration.ofMinutes(EXPIRE_AFTER_ACCESS_MINUTES))
                .removalListener((UUID uuid, PlayerStats stats, com.github.benmanes.caffeine.cache.RemovalCause cause) -> {
                    // On eviction: flush to disk so data isn't lost
                    if (stats != null && dirtySet.remove(uuid) != null) {
                        persistStats(uuid, stats);
                    }
                })
                .build();
    }

    // =========================================================================
    // Statistics file management
    // =========================================================================

    /**
     * Path to the statistics YAML file inside the plugin data folder.
     * All player stats are stored here under {@code players.<uuid>.*}.
     */
    private static final String STATS_FILENAME = "statistics.yml";

    /**
     * Live reference to the statistics YAML configuration.
     * Loaded on {@link #initialize()}, updated on every {@link #persistStats}.
     * Guarded by {@code this} for write access.
     */
    private volatile PlatformConfiguration statsConfig;

    /** Called during plugin enable — loads the persisted stats file. */
    public void initialize() {
        statsConfig = loadStatsConfig();
        logger.fine("[MythicRod] StatisticsManager initialized.");
    }

    /** Called during plugin disable — flushes all pending data. */
    public void cleanup() {
        saveAll();
        statsCache.invalidateAll();
        dirtySet.clear();
        logger.fine("[MythicRod] StatisticsManager cleaned up.");
    }

    /** Called during plugin reload — flushes, then clears in-memory state. */
    public void reload() {
        saveAll();
        statsCache.invalidateAll();
        dirtySet.clear();
        totalCatchesGlobal.set(0L);
        logger.fine("[MythicRod] StatisticsManager reloaded.");
    }

    /** @return Total catch count across all players since last reload (for bStats). */
    public long getTotalCatches() {
        return totalCatchesGlobal.get();
    }

    /**
     * Returns the top {@code limit} players by total catches.
     * Used by GUI menus that don't have async context.
     * Returns from the in-memory cache only — no DB query.
     *
     * @param limit Max entries to return.
     * @return Sorted list of PlayerStats, descending by totalCaught.
     */
    @NotNull
    public java.util.List<PlayerStats> getTopFishers(int limit) {
        return statsCache.asMap().values().stream()
                .sorted(java.util.Comparator.comparingInt(PlayerStats::getTotalCaught).reversed())
                .limit(Math.max(1, limit))
                .collect(java.util.stream.Collectors.toList());
    }

    // =========================================================================
    // Core access
    // =========================================================================

    /**
     * Returns the {@link PlayerStats} for the given player UUID.
     * Creates a new empty entry if none exists.
     *
     * <p>Thread-safe; may be called from any region thread.
     *
     * @param uuid Player UUID. Must not be null.
     * @return Non-null PlayerStats instance.
     */
    @NotNull
    public PlayerStats getOrCreate(@NotNull UUID uuid) {
        return statsCache.get(uuid, id -> new PlayerStats(id));
    }

    /**
     * Returns the {@link PlayerStats} for the given UUID, or {@code null}
     * if no entry has been created for this player yet.
     *
     * <p>Unlike {@link #getOrCreate}, this does not create a new entry.
     * Used by the API layer for read-only queries.
     *
     * @param uuid Player UUID.
     * @return Existing stats, or {@code null}.
     */
    @Nullable
    public PlayerStats getStats(@NotNull UUID uuid) {
        return statsCache.getIfPresent(uuid);
    }

    /**
     * Returns a snapshot of all currently cached {@link PlayerStats} entries.
     *
     * <p>The returned map is a point-in-time copy — mutations to the cache
     * after this call are not reflected. Safe to iterate from async threads.
     *
     * @return Immutable map of UUID → PlayerStats.
     */
    @NotNull
    public Map<UUID, PlayerStats> getAllStats() {
        return Collections.unmodifiableMap(statsCache.asMap());
    }

    // =========================================================================
    // Stat recording (hot path — called per fishing event)
    // =========================================================================

    /**
     * Records a catch for the given player, incrementing counters and marking
     * as dirty for persistence.
     *
     * @param uuid     Player UUID.
     * @param category The drop tier string (e.g. "legendary", "rare").
     */
    public void recordCatch(@NotNull UUID uuid, @NotNull String category) {
        PlayerStats stats = getOrCreate(uuid);
        stats.incrementTotalCaught();
        stats.markFished();

        switch (category.toLowerCase()) {
            case "legendary" -> stats.incrementLegendaryCaught();
            case "rare"      -> stats.incrementRareCaught();
            case "uncommon"  -> stats.incrementUncommonCaught();
            default          -> stats.incrementCommonCaught();
        }

        dirtySet.put(uuid, Boolean.TRUE);
    }

    /**
     * Records rod usage for a given rod tier.
     *
     * @param uuid    Player UUID.
     * @param rodTier The rod tier string (e.g. "basic", "advanced", "legendary").
     */
    public void recordRodUse(@NotNull UUID uuid, @NotNull String rodTier) {
        PlayerStats stats = getOrCreate(uuid);
        switch (rodTier.toLowerCase()) {
            case "legendary" -> stats.incrementLegendaryRodUses();
            case "advanced"  -> stats.incrementAdvancedRodUses();
            default          -> stats.incrementBasicRodUses();
        }
        dirtySet.put(uuid, Boolean.TRUE);
    }

    // =========================================================================
    // Persistence
    // =========================================================================

    /**
     * Flushes all dirty entries to persistent storage.
     * Must be called from the async scheduler.
     */
    public void saveAll() {
        if (dirtySet.isEmpty()) {
            return;
        }
        // Snapshot the dirty set to avoid ConcurrentModificationException
        UUID[] toSave = dirtySet.keySet().toArray(new UUID[0]);
        for (UUID uuid : toSave) {
            PlayerStats stats = statsCache.getIfPresent(uuid);
            if (stats != null) {
                persistStats(uuid, stats);
                dirtySet.remove(uuid);
            }
        }
    }

    /**
     * Pre-loads stats for a player from the YAML file (e.g., on join).
     * If no saved data exists, creates a fresh {@link PlayerStats} entry.
     * Should be called from the async scheduler.
     *
     * @param uuid       Player UUID.
     * @param playerName Player's current name.
     */
    public void loadPlayer(@NotNull UUID uuid, @NotNull String playerName) {
        statsCache.get(uuid, id -> {
            PlatformConfiguration cfg = statsConfig;
            if (cfg == null) return new PlayerStats(id, playerName);

            String base = "players." + uuid;
            String savedName = cfg.getString(base + ".name");
            int total      = cfg.getInt(base + ".total",      0);
            int common     = cfg.getInt(base + ".common",     0);
            int uncommon   = cfg.getInt(base + ".uncommon",   0);
            int rare       = cfg.getInt(base + ".rare",       0);
            int legendary  = cfg.getInt(base + ".legendary",  0);
            int rodBasic   = cfg.getInt(base + ".rod_basic",  0);
            int rodAdv     = cfg.getInt(base + ".rod_advanced", 0);
            int rodLeg     = cfg.getInt(base + ".rod_legendary", 0);

            String name = (savedName != null && !savedName.isEmpty()) ? savedName : playerName;
            PlayerStats ps = new PlayerStats(id, name);
            // Restore all counters from disk in one atomic call
            ps.loadFromPersisted(total, common, uncommon, rare, legendary,
                    rodBasic, rodAdv, rodLeg, 0L);
            return ps;
        });
    }

    /**
     * Removes a player from the cache (e.g., on quit), flushing dirty data first.
     *
     * @param uuid Player UUID.
     */
    public void unloadPlayer(@NotNull UUID uuid) {
        PlayerStats stats = statsCache.getIfPresent(uuid);
        if (stats != null && dirtySet.remove(uuid) != null) {
            persistStats(uuid, stats);
        }
        statsCache.invalidate(uuid);
    }

    // =========================================================================
    // Internal
    // =========================================================================

    /**
     * Persists a single player's stats to the {@code statistics.yml} file.
     *
     * <p>Must be called from an async thread — never from the main server thread.
     * All writes are guarded by a {@code synchronized} block on {@code this} so
     * concurrent saves from the Caffeine eviction listener don't corrupt the file.
     */
    private void persistStats(@NotNull UUID uuid, @NotNull PlayerStats stats) {
        try {
            String base = "players." + uuid;
            synchronized (this) {
                PlatformConfiguration cfg = statsConfig;
                if (cfg == null) {
                    logger.warning("[MythicRod] Cannot persist stats — statsConfig is null");
                    return;
                }
                cfg.set(base + ".name",          stats.getPlayerName());
                cfg.set(base + ".total",         stats.getTotalCaught());
                cfg.set(base + ".common",        stats.getCommonCaught());
                cfg.set(base + ".uncommon",      stats.getUncommonCaught());
                cfg.set(base + ".rare",          stats.getRareCaught());
                cfg.set(base + ".legendary",     stats.getLegendaryCaught());
                cfg.set(base + ".rod_basic",     stats.getBasicRodUses());
                cfg.set(base + ".rod_advanced",  stats.getAdvancedRodUses());
                cfg.set(base + ".rod_legendary", stats.getLegendaryRodUses());

                File statsFile = new File(plugin.getDataFolder(), STATS_FILENAME);
                cfg.save(statsFile);
            }
            logger.fine("[MythicRod] Persisted stats for " + uuid
                    + " — total=" + stats.getTotalCaught());
        } catch (Exception e) {
            logger.log(Level.WARNING, "[MythicRod] Failed to persist stats for " + uuid, e);
        }
    }

    /** Loads (or creates) the {@code statistics.yml} config from disk. */
    private @NotNull PlatformConfiguration loadStatsConfig() {
        File statsFile = new File(plugin.getDataFolder(), STATS_FILENAME);
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        try {
            return plugin.loadConfig(statsFile);
        } catch (Exception e) {
            logger.log(Level.WARNING, "[MythicRod] Could not load " + STATS_FILENAME
                    + " — starting with empty stats.", e);
            return plugin.createEmptyConfig();
        }
    }
}
