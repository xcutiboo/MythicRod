package io.xcutiboo.mythicrod.paper.api;

import io.xcutiboo.mythicrod.api.ExternalDropProvider;
import io.xcutiboo.mythicrod.api.MythicRodAPI;
import io.xcutiboo.mythicrod.api.PlayerStatSnapshot;
import io.xcutiboo.mythicrod.api.PlayerStatSnapshot.StatType;
import io.xcutiboo.mythicrod.drops.DropManager;
import io.xcutiboo.mythicrod.drops.DropRegistry;
import io.xcutiboo.mythicrod.metrics.StatisticsManager;
import io.xcutiboo.mythicrod.stats.PlayerStats;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Paper implementation of {@link MythicRodAPI}.
 *
 * <p>Registered in Bukkit's services manager at plugin enable:
 * <pre>{@code
 * getServer().getServicesManager().register(MythicRodAPI.class, this, plugin, ServicePriority.Normal);
 * }</pre>
 *
 * <p><strong>Thread safety:</strong> All mutable state in this class uses
 * {@link ConcurrentHashMap} or {@code volatile} references. Methods that
 * do not return {@link CompletableFuture} must be called from the entity's
 * owning region thread (Folia-safe).
 */
public class PaperMythicRodAPI implements MythicRodAPI {

    private final String version;
    private final DropManager dropManager;
    private final DropRegistry dropRegistry;
    private final StatisticsManager statisticsManager;

    /**
     * Thread-safe external drop provider registry.
     * Key = {@link ExternalDropProvider#getKey()}.
     */
    private final ConcurrentHashMap<String, ExternalDropProvider> externalProviders =
            new ConcurrentHashMap<>();

    public PaperMythicRodAPI(
            @NotNull String version,
            @NotNull DropManager dropManager,
            @NotNull DropRegistry dropRegistry,
            @NotNull StatisticsManager statisticsManager) {
        this.version = version;
        this.dropManager = dropManager;
        this.dropRegistry = dropRegistry;
        this.statisticsManager = statisticsManager;
    }

    // =========================================================================
    // Plugin meta
    // =========================================================================

    @Override
    @NotNull
    public String getVersion() {
        return version;
    }

    // =========================================================================
    // Drop system
    // =========================================================================

    @Override
    @NotNull
    public DropManager getDropManager() {
        return dropManager;
    }

    @Override
    @NotNull
    public DropRegistry getDropRegistry() {
        return dropRegistry;
    }

    // =========================================================================
    // External drop providers
    // =========================================================================

    @Override
    public void registerExternalDropProvider(@NotNull ExternalDropProvider provider) {
        String key = provider.getKey();
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("ExternalDropProvider key must not be null or blank");
        }
        externalProviders.put(key, provider);
    }

    @Override
    public boolean unregisterExternalDropProvider(@NotNull String key) {
        return externalProviders.remove(key) != null;
    }

    @Override
    @NotNull
    public Optional<ExternalDropProvider> getExternalDropProvider(@NotNull String key) {
        return Optional.ofNullable(externalProviders.get(key));
    }

    @Override
    @NotNull
    public List<ExternalDropProvider> getExternalDropProviders() {
        return List.copyOf(externalProviders.values());
    }

    /**
     * Returns the raw external providers map for use by the fishing pipeline.
     * This is an internal method — not exposed on the public API interface.
     *
     * @return Unmodifiable view of the providers map (by key).
     */
    @NotNull
    public Map<String, ExternalDropProvider> getExternalProvidersMap() {
        return Map.copyOf(externalProviders);
    }

    // =========================================================================
    // Statistics — CompletableFuture-backed
    // =========================================================================

    @Override
    @NotNull
    public CompletableFuture<@NotNull PlayerStatSnapshot> getPlayerStats(@NotNull UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            PlayerStats stats = statisticsManager.getStats(playerId);
            if (stats == null) {
                return PlayerStatSnapshot.empty(playerId, "Unknown");
            }
            return toSnapshot(stats);
        });
    }

    @Override
    @NotNull
    public CompletableFuture<@NotNull List<@NotNull PlayerStatSnapshot>> getTopPlayers(
            @NotNull StatType statType,
            int limit) {
        int clampedLimit = Math.max(1, Math.min(100, limit));
        return CompletableFuture.supplyAsync(() -> {
            Map<UUID, PlayerStats> allStats = statisticsManager.getAllStats();
            List<PlayerStatSnapshot> snapshots = new ArrayList<>(allStats.size());
            for (PlayerStats ps : allStats.values()) {
                snapshots.add(toSnapshot(ps));
            }
            Comparator<PlayerStatSnapshot> comparator = switch (statType) {
                case TOTAL_CAUGHT -> Comparator.comparingInt(PlayerStatSnapshot::totalCaught).reversed();
                case RARE_CAUGHT -> Comparator.comparingInt(PlayerStatSnapshot::rareCaught).reversed();
                case LEGENDARY_CAUGHT -> Comparator.comparingInt(PlayerStatSnapshot::legendaryCaught).reversed();
                case LAST_FISHED -> Comparator.comparing(PlayerStatSnapshot::lastFished).reversed();
            };
            return snapshots.stream()
                    .sorted(comparator)
                    .limit(clampedLimit)
                    .toList();
        });
    }

    @Override
    @NotNull
    public CompletableFuture<Void> flushAllStats() {
        return CompletableFuture.runAsync(() -> statisticsManager.saveAll());
    }

    // =========================================================================
    // Internal helpers
    // =========================================================================

    private PlayerStatSnapshot toSnapshot(@NotNull PlayerStats stats) {
        long lastFishedMs = stats.getLastFished();
        Instant lastFished = lastFishedMs > 0
                ? Instant.ofEpochMilli(lastFishedMs)
                : Instant.EPOCH;
        String name = stats.getPlayerName();
        if (name == null || name.isEmpty()) name = "Unknown";
        return new PlayerStatSnapshot(
                stats.getPlayerUuid(),
                name,
                stats.getTotalCaught(),
                stats.getCommonCaught(),
                stats.getUncommonCaught(),
                stats.getRareCaught(),
                stats.getLegendaryCaught(),
                stats.getBasicRodUses(),
                stats.getAdvancedRodUses(),
                stats.getLegendaryRodUses(),
                lastFished,
                Instant.now()
        );
    }
}
