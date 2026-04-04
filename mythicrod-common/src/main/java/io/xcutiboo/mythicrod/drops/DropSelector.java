package io.xcutiboo.mythicrod.drops;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

import io.xcutiboo.mythicrod.api.platform.PlatformPlayer;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Thread-safe drop selector using weighted random selection.
 *
 * <p>Thread safety: All methods are stateless with respect to mutable shared data.
 * {@code ThreadLocalRandom.current()} is called per-invocation (never stored as a field)
 * to guarantee correct behaviour across Folia region threads.
 */
@RequiredArgsConstructor
public class DropSelector {
    @NonNull
    private final Logger logger;
    // NOTE: Do NOT store ThreadLocalRandom as a field — it must be obtained per-call
    // so each Folia region thread uses its own instance.
    private boolean usePermissions = false;
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

    /**
     * Selects a drop with luck-modified weights.
     *
     * <p>A {@code luckMultiplier} &gt; 1.0 scales up the effective weight of drops
     * whose base chance is &le; 5 (rare / legendary tier), making them relatively
     * more probable without touching common or uncommon weights.  A multiplier
     * of 1.0 reproduces identical behaviour to the no-luck overload.
     *
     * @param drops          the full eligible drop pool for the category
     * @param player         the fishing player (permission + biome checks)
     * @param biomeName      current biome key, or null for global drops
     * @param luckMultiplier clamped externally to &ge; 0.01
     */
    public CustomDrop selectDrop(List<CustomDrop> drops, PlatformPlayer player,
                                  String biomeName, double luckMultiplier) {
        if (debugMode) {
            logger.fine("selectDrop called with " + (drops != null ? drops.size() : "null")
                    + " drops, luckMultiplier=" + luckMultiplier);
        }
        if (drops == null || drops.isEmpty()) {
            if (debugMode) logger.fine("Drop list is empty or null");
            return null;
        }

        List<CustomDrop> eligible = filterEligible(drops, player, biomeName);
        if (eligible.isEmpty()) {
            if (debugMode) logger.fine("No eligible drops for biome: " + biomeName);
            return null;
        }
        return selectWeightedRandomOptimized(eligible, luckMultiplier);
    }

    /** Convenience overload — luck-neutral (multiplier = 1.0). */
    public CustomDrop selectDrop(List<CustomDrop> drops, PlatformPlayer player, String biomeName) {
        return selectDrop(drops, player, biomeName, 1.0);
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
        if (!hasPermission(player, drop)) return false;
        if (!matchesBiome(drop, biomeName)) return false;
        return true;
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
        if (biomeName == null) return true;
        
        return biomes.stream().anyMatch(b -> b.equalsIgnoreCase(biomeName));
    }

    /**
     * Optimized weighted random selection using a primitive cumulative-weight array
     * and a manual binary search.
     *
     * <p>Compared to the previous {@code TreeMap<Integer,CustomDrop>} approach this
     * version avoids:
     * <ul>
     *   <li>Integer autoboxing for every entry</li>
     *   <li>TreeMap node heap allocations (one object per drop per roll)</li>
     *   <li>Pointer-chasing cache misses in the red-black tree</li>
     * </ul>
     * Time complexity: O(n) build + O(log n) binary search.<br>
     * GC pressure: two short-lived arrays per call (both immediately collectable
     * by a young-gen GC since they are never escaped).
     *
     * <p>Thread safety: {@code ThreadLocalRandom.current()} is called per-invocation
     * (never stored) so each Folia region thread uses its own instance.
     *
     * @param drops list of eligible drops, each with a non-negative {@code chance}
     * @return the selected drop, or {@code null} if all weights are zero
     */
    /**
     * Adapts luck-neutral callers to the new two-arg signature.
     * Kept for internal call sites that don't supply a multiplier.
     */
    private CustomDrop selectWeightedRandomOptimized(List<CustomDrop> drops) {
        return selectWeightedRandomOptimized(drops, 1.0);
    }

    private CustomDrop selectWeightedRandomOptimized(List<CustomDrop> drops, double luckMultiplier) {
        final int n = drops.size();

        // Parallel arrays: cumulative[i] = sum of weights for drops[0..i]
        final int[]       cumulative = new int[n];
        final CustomDrop[] dropsArr  = drops.toArray(new CustomDrop[0]);

        int total      = 0;
        int validCount = 0;
        for (int i = 0; i < n; i++) {
            int baseWeight = Math.max(0, dropsArr[i].getChance());
            // Apply luck: rare (chance ≤ 5) and legendary (chance ≤ 1) drops
            // have their effective weight multiplied by luckMultiplier so they
            // appear proportionally more often for lucky players.  Common and
            // uncommon drops are left untouched — only the relative share changes.
            int weight = baseWeight;
            if (luckMultiplier != 1.0 && baseWeight > 0) {
                if (baseWeight <= 5) {
                    weight = (int) Math.max(1, Math.round(baseWeight * luckMultiplier));
                }
            }
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
        final int roll = ThreadLocalRandom.current().nextInt(total) + 1;

        if (debugMode) {
            logger.fine("Drop selection: totalWeight=" + total
                    + ", roll=" + roll + ", validDrops=" + validCount);
        }

        // Manual binary search for the smallest cumulative[i] >= roll.
        int lo = 0, hi = n - 1;
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
