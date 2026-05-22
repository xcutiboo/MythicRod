package io.xcutiboo.mythicrod.paper.api;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import io.xcutiboo.mythicrod.api.ExternalDropProvider;
import io.xcutiboo.mythicrod.api.MythicRodAPI;
import io.xcutiboo.mythicrod.api.PlayerStatSnapshot;
import io.xcutiboo.mythicrod.api.drop.DropCatalog;
import io.xcutiboo.mythicrod.api.platform.PlatformItem;
import io.xcutiboo.mythicrod.api.platform.PlatformItemFactory;
import io.xcutiboo.mythicrod.api.platform.PlatformPlayer;
import io.xcutiboo.mythicrod.api.platform.PlatformScheduler;
import io.xcutiboo.mythicrod.drops.CustomDrop;
import io.xcutiboo.mythicrod.drops.DropConfigurationRecord;
import io.xcutiboo.mythicrod.drops.DropManager;
import io.xcutiboo.mythicrod.metrics.StatisticsManager;
import io.xcutiboo.mythicrod.stats.PlayerStats;

/// Paper runtime implementation of `MythicRodAPI`.
///
/// External plugins should retrieve and program against the stable
/// `MythicRodAPI` service. This type is public only because Bukkit's services
/// manager needs a concrete provider instance.
///
/// Registered in Bukkit's services manager during plugin enable:
///
/// ```java
/// PaperMythicRodAPI api = plugin.getApiFacade();
/// getServer().getServicesManager().register(MythicRodAPI.class, api, plugin, ServicePriority.Normal);
/// ```
///
/// ## Thread Safety
///
/// Provider registration is backed by `ConcurrentHashMap`. Methods that do not
/// return `CompletableFuture` must be called from the entity's owning region
/// thread. Future-backed methods complete on MythicRod's async scheduler, so
/// callers must schedule platform mutations back to the correct Paper/Folia
/// owner.
public class PaperMythicRodAPI implements MythicRodAPI {
    private static final String PROVIDER_LABEL = "External drop provider '";


    private final String version;
    private final Logger logger;
    private final DropManager dropManager;
    private final StatisticsManager statisticsManager;
    private final PlatformScheduler scheduler;
    private final PlatformItemFactory itemFactory;

    private final ConcurrentHashMap<String, ExternalDropProvider> externalProviders =
            new ConcurrentHashMap<>();

    public PaperMythicRodAPI(
            @NotNull String version,
            @NotNull Logger logger,
            @NotNull DropManager dropManager,
            @NotNull StatisticsManager statisticsManager,
            @NotNull PlatformScheduler scheduler,
            @NotNull PlatformItemFactory itemFactory) {
        this.version = version;
        this.logger = logger;
        this.dropManager = dropManager;
        this.statisticsManager = statisticsManager;
        this.scheduler = scheduler;
        this.itemFactory = itemFactory;
    }

    /// Internal reward-selection result used by the Paper fishing pipeline.
    ///
    /// `drop` is always the item MythicRod will announce and record.
    /// `externalItem` is present only when an external provider supplied the
    /// concrete platform item to deliver.
    public record RewardResolution(@NotNull CustomDrop drop, @Nullable PlatformItem externalItem) {
        public boolean isExternal() {
            return externalItem != null;
        }
    }

    @Override
    @NotNull
    public String getVersion() {
        return version;
    }

    @Override
    @NotNull
    public DropCatalog getDropCatalog() {
        return dropManager;
    }

    @Override
    @NotNull
    public PlatformItemFactory getItemFactory() {
        return itemFactory;
    }

    @Override
    @SuppressWarnings("java:S2589")
    public void registerExternalDropProvider(@NotNull ExternalDropProvider provider) {
        // Third-party plugins may return null from getKey() despite the @NotNull contract.
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

    public double getBaseRewardWeight(@NotNull PlatformPlayer player, @Nullable String biomeName) {
        double totalWeight = 0.0D;

        for (CustomDrop drop : dropManager.getEligibleDrops(player, biomeName)) {
            totalWeight += Math.max(0, drop.getWeight());
        }

        for (ExternalDropProvider provider : externalProviders.values()) {
            totalWeight += getProviderWeight(provider, player);
        }

        return totalWeight;
    }

    @Nullable
    public RewardResolution resolveReward(
            @NotNull PlatformPlayer player,
            @Nullable String biomeName,
            double luckMultiplier) {
        List<CustomDrop> builtInDrops = dropManager.getEligibleDrops(player, biomeName);
        List<WeightedExternalProvider> externalChoices = new ArrayList<>(externalProviders.size());

        double builtInWeight = 0.0D;
        for (CustomDrop drop : builtInDrops) {
            builtInWeight += dropManager.getEffectiveWeight(drop, luckMultiplier);
        }

        double externalWeight = 0.0D;
        for (ExternalDropProvider provider : externalProviders.values()) {
            double weight = getProviderWeight(provider, player);
            if (weight > 0.0D) {
                externalChoices.add(new WeightedExternalProvider(provider, weight));
                externalWeight += weight;
            }
        }

        double totalWeight = builtInWeight + externalWeight;
        if (totalWeight <= 0.0D) {
            return null;
        }

        while (totalWeight > 0.0D) {
            double roll = ThreadLocalRandom.current().nextDouble(totalWeight);
            double cursor = 0.0D;

            for (CustomDrop drop : builtInDrops) {
                cursor += dropManager.getEffectiveWeight(drop, luckMultiplier);
                if (roll < cursor) {
                    return new RewardResolution(drop, null);
                }
            }

            boolean rerollRequired = false;
            for (int index = 0; index < externalChoices.size(); index++) {
                WeightedExternalProvider externalChoice = externalChoices.get(index);
                cursor += externalChoice.weight();
                if (roll < cursor) {
                    PlatformItem externalItem = createExternalItem(externalChoice.provider(), player);
                    if (externalItem != null) {
                        return new RewardResolution(adaptExternalDrop(externalChoice.provider(), externalItem), externalItem);
                    }

                    totalWeight -= externalChoice.weight();
                    externalChoices.remove(index);
                    rerollRequired = true;
                    break;
                }
            }

            if (!rerollRequired) {
                break;
            }
        }

        return null;
    }

    /// Runs the single-player stats lookup away from Paper/Folia owner threads.
    ///
    /// The returned future completes on MythicRod's async scheduler. Callers
    /// must reschedule to the correct owner before touching Bukkit, Paper, or
    /// inventory state from a continuation. Missing players resolve to an empty
    /// snapshot; unexpected read failures complete the future exceptionally.
    @Override
    @NotNull
    public CompletableFuture<PlayerStatSnapshot> getPlayerStats(@NotNull UUID playerId) {
        return supplyAsync(() -> {
            PlayerStats stats = statisticsManager.getStats(playerId);
            if (stats == null) {
                return PlayerStatSnapshot.empty(playerId, "Unknown");
            }
            return toSnapshot(stats);
        });
    }

    /// Runs the stats lookup away from Paper/Folia owner threads.
    ///
    /// The returned future completes on MythicRod's async scheduler. Callers
    /// must reschedule to the correct owner before touching Bukkit, Paper, or
    /// inventory state from a continuation. Exceptions thrown by the stats
    /// manager complete the future exceptionally.
    @Override
    @NotNull
    public CompletableFuture<List<PlayerStatSnapshot>> getTopPlayers(
            @NotNull PlayerStatSnapshot.StatType statType,
            int limit) {
        int clampedLimit = Math.max(1, Math.min(100, limit));
        return supplyAsync(() -> {
            Map<UUID, PlayerStats> allStats = statisticsManager.getAllStats();
            List<PlayerStatSnapshot> snapshots = new ArrayList<>(allStats.size());
            for (PlayerStats ps : allStats.values()) {
                snapshots.add(toSnapshot(ps));
            }
            Comparator<PlayerStatSnapshot> primary = switch (statType) {
                case TOTAL_CAUGHT -> Comparator.comparingInt(PlayerStatSnapshot::totalCaught).reversed();
                case RARE_CAUGHT -> Comparator.comparingInt(PlayerStatSnapshot::rareCaught).reversed();
                case LEGENDARY_CAUGHT -> Comparator.comparingInt(PlayerStatSnapshot::legendaryCaught).reversed();
                case LAST_FISHED -> Comparator.comparing(PlayerStatSnapshot::lastFished).reversed();
            };
            Comparator<PlayerStatSnapshot> stableTiebreaker = primary
                    .thenComparing(Comparator.comparingInt(PlayerStatSnapshot::totalCaught).reversed())
                    .thenComparing(Comparator.comparing(PlayerStatSnapshot::lastFished).reversed())
                    .thenComparing(s -> s.playerUuid().toString());
            return snapshots.stream()
                    .sorted(stableTiebreaker)
                    .limit(clampedLimit)
                    .toList();
        });
    }

    /// Persists all loaded statistics on MythicRod's async scheduler.
    ///
    /// The future has no built-in timeout and may complete exceptionally when
    /// the underlying file save fails. Cancelling the future does not guarantee
    /// interruption of a save that has already started.
    @Override
    @NotNull
    public CompletableFuture<Void> flushAllStats() {
        return runAsync(statisticsManager::saveAll);
    }

    private <T> CompletableFuture<T> supplyAsync(@NotNull Supplier<T> supplier) {
        CompletableFuture<T> future = new CompletableFuture<>();
        scheduler.runAsync(() -> {
            try {
                future.complete(supplier.get());
            } catch (Throwable throwable) {
                future.completeExceptionally(throwable);
            }
        });
        return future;
    }

    private CompletableFuture<Void> runAsync(@NotNull Runnable runnable) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        scheduler.runAsync(() -> {
            try {
                runnable.run();
                future.complete(null);
            } catch (Throwable throwable) {
                future.completeExceptionally(throwable);
            }
        });
        return future;
    }

    private static final long SLOW_PROVIDER_WARN_NS = 50_000_000L;

    private double getProviderWeight(@NotNull ExternalDropProvider provider, @NotNull PlatformPlayer player) {
        long start = System.nanoTime();
        try {
            double weight = provider.getWeight(player);
            warnIfSlow(provider, "getWeight", start);
            if (Double.isNaN(weight) || Double.isInfinite(weight) || weight <= 0.0D) {
                return 0.0D;
            }
            return weight;
        } catch (Exception exception) {
            logger.log(
                Level.WARNING, exception,
                () -> PROVIDER_LABEL + safeProviderKey(provider) + "' failed during weight calculation"
            );
            return 0.0D;
        }
    }

    @Nullable
    private PlatformItem createExternalItem(@NotNull ExternalDropProvider provider, @NotNull PlatformPlayer player) {
        long start = System.nanoTime();
        try {
            PlatformItem item = provider.generateItem(player);
            warnIfSlow(provider, "generateItem", start);
            return item;
        } catch (Exception exception) {
            logger.log(
                Level.WARNING, exception,
                () -> PROVIDER_LABEL + safeProviderKey(provider) + "' failed while generating a reward item"
            );
            return null;
        }
    }

    private void warnIfSlow(@NotNull ExternalDropProvider provider, @NotNull String stage, long startNanos) {
        long elapsedNanos = System.nanoTime() - startNanos;
        if (elapsedNanos > SLOW_PROVIDER_WARN_NS) {
            long elapsedMs = elapsedNanos / 1_000_000L;
            logger.log(
                Level.WARNING,
                () -> PROVIDER_LABEL + safeProviderKey(provider) + "' " + stage
                    + " took " + elapsedMs + "ms on the fishing path; providers must stay non-blocking."
            );
        }
    }

    private CustomDrop adaptExternalDrop(@NotNull ExternalDropProvider provider, @NotNull PlatformItem externalItem) {
        String customName = safeDisplayName(provider, externalItem);
        return new CustomDrop(new DropConfigurationRecord(
            safeProviderKey(provider),
            tierToWeight(provider.getTier()),
            Math.max(1, externalItem.getAmount()),
            customName,
            externalItem.getLore(),
            0,
            externalItem.getEnchantments(),
            externalItem.getItemFlags(),
            externalItem.isGlowing(),
            null,
            List.of(),
            null
        ));
    }

    @SuppressWarnings("java:S2589")
    private String safeDisplayName(@NotNull ExternalDropProvider provider, @NotNull PlatformItem externalItem) {
        // Third-party providers may return null/blank from getDisplayName() despite the @NotNull contract.
        try {
            String providerName = provider.getDisplayName();
            if (providerName != null
                && !providerName.isBlank()
                && !"<gray>Unknown Drop</gray>".equals(providerName)) {
                return providerName;
            }
        } catch (Exception exception) {
            logger.log(
                Level.WARNING, exception,
                () -> PROVIDER_LABEL + safeProviderKey(provider) + "' failed while resolving its display name"
            );
        }

        String itemDisplayName = externalItem.getDisplayName();
        if (itemDisplayName != null && !itemDisplayName.isBlank()) {
            return itemDisplayName;
        }
        return safeProviderKey(provider);
    }

    @SuppressWarnings("java:S2589")
    private String safeProviderKey(@NotNull ExternalDropProvider provider) {
        // Third-party providers may return null/blank from getKey() despite the @NotNull contract.
        try {
            String key = provider.getKey();
            if (key != null && !key.isBlank()) {
                return key;
            }
        } catch (Exception _) {
            // Fall through to a generic identifier.
        }
        return provider.getClass().getName();
    }

    private int tierToWeight(@Nullable String tier) {
        if (tier == null) {
            return 25;
        }

        return switch (tier.trim().toLowerCase(Locale.ROOT)) {
            case "legendary" -> 1;
            case "rare" -> 5;
            case "uncommon" -> 15;
            default -> 25;
        };
    }

    /**
     * Synchronous snapshot for the given player, suitable for event handlers
     * already running on the player's owner thread. Returns {@code null} when
     * no in-memory or on-disk entry exists.
     */
    @Nullable
    public PlayerStatSnapshot snapshotFor(@NotNull UUID playerId) {
        PlayerStats stats = statisticsManager.getStats(playerId);
        return stats == null ? null : toSnapshot(stats);
    }

    @SuppressWarnings("java:S2589")
    private PlayerStatSnapshot toSnapshot(@NotNull PlayerStats stats) {
        long lastFishedMs = stats.getLastFished();
        Instant lastFished = lastFishedMs > 0
                ? Instant.ofEpochMilli(lastFishedMs)
                : Instant.EPOCH;
        // Persisted entries from older versions can carry a null/empty playerName.
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

    private record WeightedExternalProvider(@NotNull ExternalDropProvider provider, double weight) {
    }
}
