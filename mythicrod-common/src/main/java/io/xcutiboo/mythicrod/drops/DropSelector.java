package io.xcutiboo.mythicrod.drops;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

import io.xcutiboo.mythicrod.api.platform.PlatformPlayer;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/// Thread-safe drop selector using weighted random selection.
///
/// Thread safety: All methods are stateless with respect to mutable shared data.
/// `ThreadLocalRandom.current()` is called per-invocation (never stored as a field)
/// to guarantee correct behaviour across Folia region threads.
@RequiredArgsConstructor
public class DropSelector {
    private static final double MIN_LUCK_MULTIPLIER = 0.01D;
    private static final double MAX_LUCK_MULTIPLIER = 10.0D;

    @NonNull
    private final Logger logger;
    // ThreadLocalRandom must be obtained per call so each Folia region thread uses
    // its own instance.
    private boolean usePermissions = false;
    private boolean useBiomeSpecificDrops = true;
    private boolean debugMode = false;

    public DropSelector(@NonNull Logger logger, boolean usePermissions) {
        this.logger = logger;
        this.usePermissions = usePermissions;
    }

    public void setDebugMode(boolean debug) {
        this.debugMode = debug;
    }

    public void setUsePermissions(boolean use) {
        this.usePermissions = use;
    }

    public void setUseBiomeSpecificDrops(boolean use) {
        this.useBiomeSpecificDrops = use;
    }

    /// Selects a drop with luck-modified weights.
    ///
    /// A `luckMultiplier` &gt; 1.0 scales up the effective weight of drops
    /// whose base weight is &le; 5 (rare / legendary tier), making them relatively
    /// more probable without touching common or uncommon weights. A multiplier
    /// of 1.0 reproduces identical behaviour to the no-luck overload.
    ///
    /// @param drops          the full eligible drop pool for the category
    /// @param player         the fishing player (permission + biome checks)
    /// @param biomeName      current biome key, or null for global drops
    /// @param luckMultiplier clamped externally to &ge; 0.01
    public CustomDrop selectDrop(List<CustomDrop> drops, PlatformPlayer player,
                                  String biomeName, double luckMultiplier) {
        double safeLuckMultiplier = sanitizeLuckMultiplier(luckMultiplier);
        if (debugMode) {
            logger.fine(() -> "selectDrop called with " + (drops != null ? drops.size() : "null")
                    + " drops, luckMultiplier=" + safeLuckMultiplier);
        }
        if (drops == null || drops.isEmpty()) {
            if (debugMode) logger.fine("Drop list is empty or null");
            return null;
        }

        List<CustomDrop> eligible = filterEligible(drops, player, biomeName);
        if (eligible.isEmpty()) {
            if (debugMode) logger.fine(() -> "No eligible drops for biome: " + biomeName);
            return null;
        }
        return selectWeightedRandomOptimized(eligible, safeLuckMultiplier);
    }

    private double sanitizeLuckMultiplier(double luckMultiplier) {
        if (Double.isNaN(luckMultiplier) || Double.isInfinite(luckMultiplier)) {
            if (debugMode) {
                logger.fine(() -> "Invalid luck multiplier " + luckMultiplier + ", using default 1.0");
            }
            return 1.0D;
        }

        double clamped = Math.clamp(luckMultiplier, MIN_LUCK_MULTIPLIER, MAX_LUCK_MULTIPLIER);
        if (debugMode && clamped != luckMultiplier) {
            logger.fine(() -> "Luck multiplier clamped from " + luckMultiplier + " to " + clamped);
        }
        return clamped;
    }

    /// Convenience overload for luck-neutral selection.
    public CustomDrop selectDrop(List<CustomDrop> drops, PlatformPlayer player, String biomeName) {
        return selectDrop(drops, player, biomeName, 1.0);
    }

    /// Returns the eligible subset of the supplied drop list for the given
    /// player and biome context.
    ///
    /// @param drops     Full drop pool.
    /// @param player    Player being evaluated.
    /// @param biomeName Current biome key, or `null` if unavailable.
    /// @return Immutable snapshot of eligible drops.
    public List<CustomDrop> getEligibleDrops(List<CustomDrop> drops, PlatformPlayer player, String biomeName) {
        if (drops == null || drops.isEmpty()) {
            return List.of();
        }
        return List.copyOf(filterEligible(drops, player, biomeName));
    }

    /// Computes the effective roll weight for a base drop weight after applying
    /// MythicRod's luck rules.
    ///
    /// @param baseWeight      Raw configured weight.
    /// @param luckMultiplier  Multiplier from the reward-roll event.
    /// @return Non-negative effective weight used by the selector.
    public int getEffectiveWeight(int baseWeight, double luckMultiplier) {
        int safeBaseWeight = Math.max(0, baseWeight);
        double safeLuckMultiplier = sanitizeLuckMultiplier(luckMultiplier);

        if (safeLuckMultiplier != 1.0D && safeBaseWeight > 0 && safeBaseWeight <= 5) {
            return (int) Math.max(1, Math.round(safeBaseWeight * safeLuckMultiplier));
        }
        return safeBaseWeight;
    }

    private List<CustomDrop> filterEligible(List<CustomDrop> drops, PlatformPlayer player, String biomeName) {
        List<CustomDrop> eligible = new ArrayList<>();

        for (CustomDrop drop : drops) {
            if (isEligible(drop, player, biomeName)) {
                eligible.add(drop);
            }
        }

        return eligible;
    }

    private boolean isEligible(CustomDrop drop, PlatformPlayer player, String biomeName) {
        return hasPermission(player, drop) && matchesBiome(drop, biomeName);
    }

    private boolean hasPermission(PlatformPlayer player, CustomDrop drop) {
        if (!usePermissions) return true;

        String permission = drop.getPermission();
        if (permission == null || permission.isEmpty()) return true;

        return player.hasPermission(permission);
    }

    private boolean matchesBiome(CustomDrop drop, String biomeName) {
        List<String> biomes = drop.getBiomes();
        if (biomes == null || biomes.isEmpty()) return true;
        if (!useBiomeSpecificDrops || biomeName == null || biomeName.isBlank()) return false;

        return biomes.stream().anyMatch(b -> b.equalsIgnoreCase(biomeName));
    }

    /// Selects from eligible drops using a primitive cumulative-weight array and
    /// a manual binary search.
    ///
    /// The implementation avoids per-roll map nodes and boxed cumulative
    /// weights. It still builds short-lived arrays for each selection because
    /// the eligible set depends on player, biome and luck context.
    ///
    /// Thread safety: `ThreadLocalRandom.current()` is called per
    /// invocation so each Folia region thread uses its own random source.
    ///
    /// @param drops list of eligible drops
    /// @param luckMultiplier multiplier applied to rare drop weights
    /// @return the selected drop, or `null` if all weights are zero
    private CustomDrop selectWeightedRandomOptimized(List<CustomDrop> drops, double luckMultiplier) {
        final int n = drops.size();

        // Parallel arrays: cumulative[i] = sum of weights for drops[0..i]
        final int[]       cumulative = new int[n];
        final CustomDrop[] dropsArr  = drops.toArray(new CustomDrop[0]);

        int total      = 0;
        int validCount = 0;
        for (int i = 0; i < n; i++) {
            int baseWeight = Math.max(0, dropsArr[i].getWeight());
            int weight = getEffectiveWeight(baseWeight, luckMultiplier);
            total += weight;
            cumulative[i] = total;
            if (weight > 0) validCount++;
        }

        if (total == 0) {
            if (debugMode) logger.fine("No drops with positive weight found");
            return null;
        }

        // Roll in [1, total] so that ceilingEntry semantics are preserved:
        // the selected index is the first i where cumulative[i] >= roll.
        // ThreadLocalRandom is intentional: drop selection is a gameplay roll, not
        // a security-sensitive draw. Predictability of the next drop carries no
        // security risk; SecureRandom would waste entropy on a hot fishing path.
        @SuppressWarnings("java:S2245")
        final int roll = ThreadLocalRandom.current().nextInt(total) + 1;

        if (debugMode) {
            final int totalFinal = total;
            final int validCountFinal = validCount;
            logger.fine(() -> "Drop selection: totalWeight=" + totalFinal
                    + ", roll=" + roll + ", validDrops=" + validCountFinal);
        }

        // Manual binary search for the smallest cumulative[i] >= roll.
        int lo = 0;
        int hi = n - 1;
        while (lo < hi) {
            final int mid = (lo + hi) >>> 1;
            if (cumulative[mid] < roll) {
                lo = mid + 1;
            } else {
                hi = mid;
            }
        }
        return dropsArr[lo];
    }
}
