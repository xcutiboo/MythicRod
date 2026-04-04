package io.xcutiboo.mythicrod.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.xcutiboo.mythicrod.drops.CustomDrop;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Caffeine-backed cache layer for MythicRod.
 *
 * <h3>What is cached here</h3>
 * <ul>
 *   <li><strong>dropCache</strong>  — resolved drop lists keyed by {@code "biome:tier"}.</li>
 *   <li><strong>localeCache</strong> — per-locale message strings.</li>
 * </ul>
 *
 * <h3>What is NOT cached here (and why)</h3>
 * <p>A {@code playerStatsCache} was previously declared with type
 * {@code Cache<String, io.xcutiboo.mythicrod.stats.PlayerStats>}.
 * {@link io.xcutiboo.mythicrod.metrics.StatisticsManager} uses its own <em>inner</em>
 * class {@code StatisticsManager.PlayerStats} — a completely different type.
 * As a result no value was ever successfully stored or retrieved through that cache;
 * it was permanently dead code. {@code StatisticsManager} already maintains its own
 * {@link java.util.concurrent.ConcurrentHashMap} of live {@code PlayerStats} objects,
 * which is the correct single source of truth for per-player statistics.
 */
public class MythicRodCache {

    private final Cache<String, List<CustomDrop>> dropCache;
    private final Cache<String, String> localeCache;

    private volatile long cacheHits   = 0;
    private volatile long cacheMisses = 0;

    public MythicRodCache() {
        this.dropCache = Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .maximumSize(500)
            .recordStats()
            .build();

        this.localeCache = Caffeine.newBuilder()
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .maximumSize(2000)
            .recordStats()
            .build();
    }

    // -------------------------------------------------------------------------
    // Drop cache
    // -------------------------------------------------------------------------

    public Optional<List<CustomDrop>> getDrops(String biome, String rodTier) {
        String key = biome + ":" + rodTier;
        List<CustomDrop> drops = dropCache.getIfPresent(key);
        if (drops != null) {
            cacheHits++;
            return Optional.of(drops);
        }
        cacheMisses++;
        return Optional.empty();
    }

    public void cacheDrops(String biome, String rodTier, List<CustomDrop> drops) {
        dropCache.put(biome + ":" + rodTier, drops);
    }

    // -------------------------------------------------------------------------
    // Locale / message cache
    // -------------------------------------------------------------------------

    public Optional<String> getLocalizedString(String locale, String key) {
        String cacheKey = locale + ":" + key;
        String value = localeCache.getIfPresent(cacheKey);
        if (value != null) {
            cacheHits++;
            return Optional.of(value);
        }
        cacheMisses++;
        return Optional.empty();
    }

    public void cacheLocalizedString(String locale, String key, String value) {
        localeCache.put(locale + ":" + key, value);
    }

    // -------------------------------------------------------------------------
    // Invalidation
    // -------------------------------------------------------------------------

    public void invalidateAll() {
        dropCache.invalidateAll();
        localeCache.invalidateAll();
        cacheHits   = 0;
        cacheMisses = 0;
    }

    public void invalidateDropCache() {
        dropCache.invalidateAll();
    }

    // -------------------------------------------------------------------------
    // Statistics
    // -------------------------------------------------------------------------

    public CacheStats getStats() {
        return new CacheStats(
            cacheHits,
            cacheMisses,
            dropCache.estimatedSize(),
            localeCache.estimatedSize(),
            calculateHitRate()
        );
    }

    private double calculateHitRate() {
        long total = cacheHits + cacheMisses;
        if (total == 0) return 0.0;
        return (double) cacheHits / total * 100.0;
    }

    /**
     * Immutable snapshot of cache statistics.
     *
     * <p>The {@code playerStatsCacheSize} field was removed because the backing
     * cache was permanently dead (type mismatch — see class-level Javadoc).
     */
    public record CacheStats(
        long   hits,
        long   misses,
        long   dropCacheSize,
        long   localeCacheSize,
        double hitRate
    ) {
        @Override
        public String toString() {
            return String.format(
                "CacheStats{hits=%d, misses=%d, hitRate=%.2f%%, drops=%d, locale=%d}",
                hits, misses, hitRate, dropCacheSize, localeCacheSize
            );
        }
    }
}
