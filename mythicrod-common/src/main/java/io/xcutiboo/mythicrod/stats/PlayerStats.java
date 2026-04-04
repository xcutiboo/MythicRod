package io.xcutiboo.mythicrod.stats;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-safe player fishing statistics container.
 *
 * <p>All counters use atomic types so increments are safe to call from
 * any Folia region thread without external synchronization.
 *
 * <p>This class is intentionally mutable — callers accumulate stats here
 * over the player's session. Snapshots for external consumers are created
 * via {@link io.xcutiboo.mythicrod.api.PlayerStatSnapshot}.
 */
public final class PlayerStats {

    private final @NotNull UUID playerUuid;
    private final @NotNull String playerName;

    // ---- Catch counters ----
    private final AtomicInteger totalCaught      = new AtomicInteger(0);
    private final AtomicInteger commonCaught     = new AtomicInteger(0);
    private final AtomicInteger uncommonCaught   = new AtomicInteger(0);
    private final AtomicInteger rareCaught       = new AtomicInteger(0);
    private final AtomicInteger legendaryCaught  = new AtomicInteger(0);

    // ---- Rod usage counters ----
    private final AtomicInteger basicRodUses     = new AtomicInteger(0);
    private final AtomicInteger advancedRodUses  = new AtomicInteger(0);
    private final AtomicInteger legendaryRodUses = new AtomicInteger(0);

    // ---- Timestamps ----
    private final AtomicLong lastFished = new AtomicLong(0L);

    public PlayerStats(@NotNull UUID playerUuid, @NotNull String playerName) {
        this.playerUuid = playerUuid;
        this.playerName = playerName;
    }

    /** Backward-compatible constructor for code that only has a UUID. */
    public PlayerStats(@NotNull UUID playerUuid) {
        this(playerUuid, "Unknown");
    }

    // =========================================================================
    // Getters
    // =========================================================================

    @NotNull
    public UUID getPlayerUuid() { return playerUuid; }

    @NotNull
    public String getPlayerName() { return playerName; }

    public int getTotalCaught()      { return totalCaught.get(); }
    public int getCommonCaught()     { return commonCaught.get(); }
    public int getUncommonCaught()   { return uncommonCaught.get(); }
    public int getRareCaught()       { return rareCaught.get(); }
    public int getLegendaryCaught()  { return legendaryCaught.get(); }

    public int getBasicRodUses()     { return basicRodUses.get(); }
    public int getAdvancedRodUses()  { return advancedRodUses.get(); }
    public int getLegendaryRodUses() { return legendaryRodUses.get(); }

    /** Returns the epoch-millisecond timestamp of the last catch, or {@code 0} if never. */
    public long getLastFished()      { return lastFished.get(); }

    /**
     * Returns a material-count breakdown.
     *
     * <p>The new statistics model tracks tier categories (common/uncommon/rare/legendary)
     * rather than individual material IDs. This method is provided for backward-
     * compatibility with GUI code written against the old inner-class model, and
     * returns the tier breakdown as a readable map.
     *
     * @return An immutable map of tier-name → catch-count (never null, may be empty).
     */
    @NotNull
    public Map<String, Integer> getMaterialCounts() {
        Map<String, Integer> counts = new java.util.LinkedHashMap<>(4);
        int common    = commonCaught.get();
        int uncommon  = uncommonCaught.get();
        int rare      = rareCaught.get();
        int legendary = legendaryCaught.get();
        if (legendary > 0) counts.put("Legendary", legendary);
        if (rare      > 0) counts.put("Rare",      rare);
        if (uncommon  > 0) counts.put("Uncommon",  uncommon);
        if (common    > 0) counts.put("Common",    common);
        return Collections.unmodifiableMap(counts);
    }

    // =========================================================================
    // Mutators
    // =========================================================================

    public void incrementTotalCaught()      { totalCaught.incrementAndGet(); }
    public void incrementCommonCaught()     { commonCaught.incrementAndGet(); }
    public void incrementUncommonCaught()   { uncommonCaught.incrementAndGet(); }
    public void incrementRareCaught()       { rareCaught.incrementAndGet(); }
    public void incrementLegendaryCaught()  { legendaryCaught.incrementAndGet(); }

    public void incrementBasicRodUses()     { basicRodUses.incrementAndGet(); }
    public void incrementAdvancedRodUses()  { advancedRodUses.incrementAndGet(); }
    public void incrementLegendaryRodUses() { legendaryRodUses.incrementAndGet(); }

    /** Records the current timestamp as the last fishing time. */
    public void markFished()                { lastFished.set(System.currentTimeMillis()); }

    // =========================================================================
    // Bulk load (used when restoring persisted data)
    // =========================================================================

    /**
     * Overwrites all counters with persisted values. Should only be called
     * once during data load — never during active gameplay.
     */
    public void loadFromPersisted(
            int total, int common, int uncommon, int rare, int legendary,
            int basic, int advanced, int legendaryRod,
            long lastFishedMs) {
        totalCaught.set(total);
        commonCaught.set(common);
        uncommonCaught.set(uncommon);
        rareCaught.set(rare);
        legendaryCaught.set(legendary);
        basicRodUses.set(basic);
        advancedRodUses.set(advanced);
        legendaryRodUses.set(legendaryRod);
        lastFished.set(lastFishedMs);
    }

    @Override
    public String toString() {
        return "PlayerStats{uuid=" + playerUuid
                + ", total=" + totalCaught
                + ", rare=" + rareCaught
                + ", legendary=" + legendaryCaught + "}";
    }
}
