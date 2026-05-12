package io.xcutiboo.mythicrod.metrics;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.RemovalListener;

import io.xcutiboo.mythicrod.api.platform.PlatformConfiguration;
import io.xcutiboo.mythicrod.internal.runtime.MythicRodRuntime;
import io.xcutiboo.mythicrod.stats.PlayerStats;

/**
 * Manages in-memory player fishing statistics with Caffeine TTL-bounded caching.
 *
 * <h2>Thread Safety</h2>
 * <ul>
 *   <li>The active-players cache uses Caffeine and is safe for concurrent access.</li>
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

    private final MythicRodRuntime runtime;
    private final Cache<UUID, PlayerStats> statsCache;
    private final ConcurrentHashMap<UUID, Boolean> dirtySet = new ConcurrentHashMap<>();

    /** Total catches across all players since last reload; used by bStats. */
    private final AtomicLong totalCatchesGlobal = new AtomicLong(0L);

    public StatisticsManager(@NotNull MythicRodRuntime runtime) {
        this.runtime = runtime;
        AtomicReference<Cache<UUID, PlayerStats>> cacheRef = new AtomicReference<>();
        this.statsCache = Caffeine.newBuilder()
                .initialCapacity(INITIAL_CAPACITY)
                .expireAfterAccess(Duration.ofMinutes(EXPIRE_AFTER_ACCESS_MINUTES))
                .removalListener(new StatsCacheRemovalListener(cacheRef))
                .build();
        cacheRef.set(this.statsCache);
    }

    private final class StatsCacheRemovalListener implements RemovalListener<UUID, PlayerStats> {
        private final AtomicReference<Cache<UUID, PlayerStats>> cacheRef;

        private StatsCacheRemovalListener(AtomicReference<Cache<UUID, PlayerStats>> cacheRef) {
            this.cacheRef = cacheRef;
        }

        @Override
        @SuppressWarnings("null")
        public void onRemoval(@Nullable UUID uuid, @Nullable PlayerStats stats, @NotNull RemovalCause cause) {
            if (uuid != null && stats != null && dirtySet.containsKey(uuid) && !persistStats(uuid, stats)) {
                Cache<UUID, PlayerStats> activeCache = cacheRef.get();
                if (activeCache != null) {
                    activeCache.put(uuid, stats);
                }
                dirtySet.put(uuid, Boolean.TRUE);
                logger().fine(() -> "Restored evicted dirty stats for " + uuid + " after persistence failure");
                return;
            }
            if (uuid != null && stats != null) {
                dirtySet.remove(uuid);
            }
        }
    }

    /**
     * Path to the statistics YAML file inside the plugin data folder.
     * All player stats are stored here under {@code players.<uuid>.*}.
     */
    private static final String STATS_FILENAME = "statistics.yml";

    /**
     * Live reference to the statistics YAML configuration.
     * Guarded by {@code this} for write access.
     */
    private volatile PlatformConfiguration statsConfig;

    public void initialize() {
        statsConfig = loadStatsConfig();
        logger().fine("Statistics manager initialized");
    }

    public void cleanup() {
        saveAll();
        statsCache.invalidateAll();
        dirtySet.clear();
        logger().fine("Statistics manager cleaned up");
    }

    public void reload() {
        saveAll();
        statsConfig = loadStatsConfig();
        statsCache.invalidateAll();
        dirtySet.clear();
        totalCatchesGlobal.set(0L);
        logger().fine("Statistics manager reloaded");
    }

    /** @return Total catch count across all players since last reload (for bStats). */
    public long getTotalCatches() {
        return totalCatchesGlobal.get();
    }

    /**
     * Returns the top {@code limit} players by total catches.
     * Used by GUI menus that do not have async context.
     *
     * @param limit Max entries to return.
     * @return Sorted list of PlayerStats, descending by totalCaught.
     */
    @NotNull
    public List<PlayerStats> getTopFishers(int limit) {
        return getAllStats().values().stream()
                .sorted(Comparator.comparingInt(PlayerStats::getTotalCaught).reversed())
                .limit(Math.max(1, limit))
                .collect(Collectors.toList());
    }

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
        return statsCache.get(uuid, id -> {
            PlayerStats persistedStats = loadPersistedStats(id, null);
            return persistedStats != null ? persistedStats : new PlayerStats(id, "Unknown");
        });
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
        PlayerStats cachedStats = statsCache.getIfPresent(uuid);
        if (cachedStats != null) {
            return cachedStats;
        }
        return loadPersistedStats(uuid, null);
    }

    /**
     * Returns a snapshot of all currently cached {@link PlayerStats} entries.
     *
     * <p>The returned map is a point-in-time copy. Mutations to the cache
     * after this call are not reflected. Safe to iterate from async threads.
     *
     * @return Immutable map of UUID → PlayerStats.
     */
    @NotNull
    public Map<UUID, PlayerStats> getAllStats() {
        Map<UUID, PlayerStats> combinedStats = snapshotPersistedStats();
        combinedStats.putAll(statsCache.asMap());
        return Collections.unmodifiableMap(combinedStats);
    }

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
        totalCatchesGlobal.incrementAndGet();

        switch (category.toLowerCase(Locale.ROOT)) {
            case "legendary" -> stats.incrementLegendaryCaught();
            case "rare"      -> stats.incrementRareCaught();
            case "uncommon"  -> stats.incrementUncommonCaught();
            default          -> stats.incrementCommonCaught();
        }

        dirtySet.put(uuid, Boolean.TRUE);
    }

    public void recordRodUse(@NotNull UUID uuid, @NotNull String rodTier) {
        PlayerStats stats = getOrCreate(uuid);
        switch (rodTier.toLowerCase(Locale.ROOT)) {
            case "legendary" -> stats.incrementLegendaryRodUses();
            case "advanced"  -> stats.incrementAdvancedRodUses();
            default          -> stats.incrementBasicRodUses();
        }
        dirtySet.put(uuid, Boolean.TRUE);
    }

    /**
     * Flushes all dirty entries to persistent storage.
     * Must be called from the async scheduler.
     */
    public void saveAll() {
        if (dirtySet.isEmpty()) {
            return;
        }

        // Caffeine eviction can touch the dirty set while this batch is saving.
        UUID[] toSave = dirtySet.keySet().toArray(UUID[]::new);

        try {
            int persistedEntries = 0;
            ArrayList<UUID> persistedIds = new ArrayList<>(toSave.length);
            synchronized (this) {
                PlatformConfiguration cfg = statsConfig;
                if (cfg == null) {
                    logger().warning("Cannot persist stats batch because statsConfig is null");
                    return;
                }

                for (UUID uuid : toSave) {
                    PlayerStats stats = statsCache.getIfPresent(uuid);
                    if (stats == null) {
                        continue;
                    }

                    writeStats(cfg, uuid, stats);
                    persistedIds.add(uuid);
                    persistedEntries++;
                }

                if (persistedEntries == 0) {
                    return;
                }

                File statsFile = new File(runtime.getDataFolder(), STATS_FILENAME);
                cfg.save(statsFile);
                persistedIds.forEach(dirtySet::remove);
                int savedEntries = persistedEntries;
                logger().fine(() -> "Persisted " + savedEntries + " dirty stats entries");
            }
        } catch (IOException | RuntimeException e) {
            logger().log(Level.WARNING, "Failed to persist dirty statistics batch", e);
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
            PlayerStats persistedStats = loadPersistedStats(id, playerName);
            return persistedStats != null ? persistedStats : new PlayerStats(id, playerName);
        });
    }

    /**
     * Removes a player from the cache (e.g., on quit), flushing dirty data first.
     *
     * @param uuid Player UUID.
     */
    public void unloadPlayer(@NotNull UUID uuid) {
        PlayerStats stats = statsCache.getIfPresent(uuid);
        if (stats != null && dirtySet.containsKey(uuid)) {
            if (!persistStats(uuid, stats)) {
                logger().fine(() -> "Keeping dirty stats for " + uuid + " in cache after unload persistence failure");
                return;
            }
            dirtySet.remove(uuid);
        }
        statsCache.invalidate(uuid);
    }

    /**
     * Persists a single player's stats to the {@code statistics.yml} file.
     *
     * <p>Must be called from an async thread, never from the main server thread.
     * All writes are guarded by a {@code synchronized} block on {@code this} so
     * concurrent saves from the Caffeine eviction listener don't corrupt the file.
     */
    private boolean persistStats(@NotNull UUID uuid, @NotNull PlayerStats stats) {
        try {
            synchronized (this) {
                PlatformConfiguration cfg = statsConfig;
                if (cfg == null) {
                    logger().warning("Cannot persist stats because statsConfig is null");
                    return false;
                }
                writeStats(cfg, uuid, stats);

                File statsFile = new File(runtime.getDataFolder(), STATS_FILENAME);
                cfg.save(statsFile);
            }
            logger().fine(() -> "Persisted stats for " + uuid + " (total=" + stats.getTotalCaught() + ")");
            return true;
        } catch (IOException | RuntimeException e) {
            logger().log(Level.WARNING, "Failed to persist stats for " + uuid, e);
            return false;
        }
    }

    private void writeStats(@NotNull PlatformConfiguration cfg, @NotNull UUID uuid, @NotNull PlayerStats stats) {
        String base = "players." + uuid;
        cfg.set(base + ".name",          stats.getPlayerName());
        cfg.set(base + ".total",         stats.getTotalCaught());
        cfg.set(base + ".common",        stats.getCommonCaught());
        cfg.set(base + ".uncommon",      stats.getUncommonCaught());
        cfg.set(base + ".rare",          stats.getRareCaught());
        cfg.set(base + ".legendary",     stats.getLegendaryCaught());
        cfg.set(base + ".rod_basic",     stats.getBasicRodUses());
        cfg.set(base + ".rod_advanced",  stats.getAdvancedRodUses());
        cfg.set(base + ".rod_legendary", stats.getLegendaryRodUses());
        cfg.set(base + ".last_fished",   stats.getLastFished());
    }

    private Logger logger() {
        return runtime.getLogger();
    }

    @Nullable
    private PlayerStats loadPersistedStats(@NotNull UUID uuid, @Nullable String fallbackName) {
        PlatformConfiguration cfg = statsConfig;
        if (cfg == null) {
            return fallbackName != null ? new PlayerStats(uuid, fallbackName) : null;
        }

        String base = "players." + uuid;
        String savedName;
        int total;
        int common;
        int uncommon;
        int rare;
        int legendary;
        int rodBasic;
        int rodAdvanced;
        int rodLegendary;
        long lastFishedMs;

        synchronized (this) {
            if (!cfg.contains(base + ".total")
                && !cfg.contains(base + ".common")
                && !cfg.contains(base + ".rare")
                && !cfg.contains(base + ".legendary")) {
                return fallbackName != null ? new PlayerStats(uuid, fallbackName) : null;
            }

            savedName = cfg.getString(base + ".name");
            total = cfg.getInt(base + ".total", 0);
            common = cfg.getInt(base + ".common", 0);
            uncommon = cfg.getInt(base + ".uncommon", 0);
            rare = cfg.getInt(base + ".rare", 0);
            legendary = cfg.getInt(base + ".legendary", 0);
            rodBasic = cfg.getInt(base + ".rod_basic", 0);
            rodAdvanced = cfg.getInt(base + ".rod_advanced", 0);
            rodLegendary = cfg.getInt(base + ".rod_legendary", 0);
            lastFishedMs = (long) cfg.getDouble(base + ".last_fished", 0.0D);
        }

        String name = (savedName != null && !savedName.isEmpty())
            ? savedName
            : (fallbackName != null && !fallbackName.isEmpty() ? fallbackName : "Unknown");
        PlayerStats playerStats = new PlayerStats(uuid, name);
        playerStats.loadFromPersisted(total, common, uncommon, rare, legendary,
            rodBasic, rodAdvanced, rodLegendary, lastFishedMs);
        return playerStats;
    }

    @NotNull
    private Map<UUID, PlayerStats> snapshotPersistedStats() {
        PlatformConfiguration cfg = statsConfig;
        if (cfg == null) {
            return new HashMap<>();
        }

        Map<UUID, PlayerStats> persistedStats = new HashMap<>();
        Set<String> playerIds;
        synchronized (this) {
            playerIds = cfg.getKeys("players", false);
        }

        for (String playerId : playerIds) {
            try {
                UUID uuid = UUID.fromString(playerId);
                PlayerStats playerStats = loadPersistedStats(uuid, null);
                if (playerStats != null) {
                    persistedStats.put(uuid, playerStats);
                }
            } catch (IllegalArgumentException e) {
                logger().log(Level.WARNING, "Ignoring invalid statistics entry for " + playerId, e);
            }
        }
        return persistedStats;
    }

    /** Loads (or creates) the {@code statistics.yml} config from disk. */
    private @NotNull PlatformConfiguration loadStatsConfig() {
        File statsFile = new File(runtime.getDataFolder(), STATS_FILENAME);
        if (!runtime.getDataFolder().exists()) {
            runtime.getDataFolder().mkdirs();
        }
        try {
            return runtime.loadConfig(statsFile);
        } catch (Exception e) {
            logger().log(Level.WARNING, "Could not load " + STATS_FILENAME + "; starting with empty stats.", e);
            return runtime.createEmptyConfig();
        }
    }
}
