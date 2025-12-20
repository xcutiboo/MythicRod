package io.xcutiboo.mythicrod.stats;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.jetbrains.annotations.NotNull;

/**
 * Thread-safe player fishing statistics container.
 *
 * <p>All counters use atomic types so increments are safe to call from
 * any Folia region thread without external synchronization.
 *
 * <p>Runtime code increments this object during play. Public API callers receive
 * immutable {@link io.xcutiboo.mythicrod.api.PlayerStatSnapshot} copies.
 */
public final class PlayerStats {

    private final @NotNull UUID playerUuid;
    private final @NotNull String playerName;

    private final AtomicInteger totalCaught      = new AtomicInteger(0);
    private final AtomicInteger commonCaught     = new AtomicInteger(0);
    private final AtomicInteger uncommonCaught   = new AtomicInteger(0);
    private final AtomicInteger rareCaught       = new AtomicInteger(0);
    private final AtomicInteger legendaryCaught  = new AtomicInteger(0);

    private final AtomicInteger basicRodUses     = new AtomicInteger(0);
    private final AtomicInteger advancedRodUses  = new AtomicInteger(0);
    private final AtomicInteger legendaryRodUses = new AtomicInteger(0);

    private final AtomicLong lastFished = new AtomicLong(0L);

    public PlayerStats(@NotNull UUID playerUuid, @NotNull String playerName) {
        this.playerUuid = playerUuid;
        this.playerName = playerName;
    }

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

    /** Returns non-zero catch counts by rarity tier, ordered from rarest to common. */
    @NotNull
    public Map<String, Integer> getTierCounts() {
        Map<String, Integer> counts = LinkedHashMap.newLinkedHashMap(4);
        int common    = commonCaught.get();
        int uncommon  = uncommonCaught.get();
        int rare      = rareCaught.get();
        int legendary = legendaryCaught.get();
        if (legendary > 0) counts.put("legendary", legendary);
        if (rare      > 0) counts.put("rare",      rare);
        if (uncommon  > 0) counts.put("uncommon",  uncommon);
        if (common    > 0) counts.put("common",    common);
        return Collections.unmodifiableMap(counts);
    }

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

    /**
     * Resets every counter to zero. The player UUID and name are preserved so
     * subsequent updates land on the same entry.
     */
    public void reset() {
        totalCaught.set(0);
        commonCaught.set(0);
        uncommonCaught.set(0);
        rareCaught.set(0);
        legendaryCaught.set(0);
        basicRodUses.set(0);
        advancedRodUses.set(0);
        legendaryRodUses.set(0);
        lastFished.set(0L);
    }

    /**
     * Overwrites all counters with persisted values.
     *
     * <p>Call this while loading data, before the object is exposed to active
     * gameplay paths.
     */
    public void loadFromPersisted(PersistedSnapshot snapshot) {
        totalCaught.set(snapshot.total());
        commonCaught.set(snapshot.common());
        uncommonCaught.set(snapshot.uncommon());
        rareCaught.set(snapshot.rare());
        legendaryCaught.set(snapshot.legendary());
        basicRodUses.set(snapshot.basic());
        advancedRodUses.set(snapshot.advanced());
        legendaryRodUses.set(snapshot.legendaryRod());
        lastFished.set(snapshot.lastFishedMs());
    }

    /**
     * Value object describing the persisted shape of a {@link PlayerStats}
     * row as it appears on disk.
     */
    public record PersistedSnapshot(
        int total,
        int common,
        int uncommon,
        int rare,
        int legendary,
        int basic,
        int advanced,
        int legendaryRod,
        long lastFishedMs
    ) {}

    @Override
    public String toString() {
        return "PlayerStats{uuid=" + playerUuid
                + ", total=" + totalCaught
                + ", rare=" + rareCaught
                + ", legendary=" + legendaryCaught + "}";
    }
}
